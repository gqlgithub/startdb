package imp;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import utils.DynamicSliceOutput;
import utils.MergingIterator;
import utils.Slice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-01 18:05
 **/
public class VersionSet {
    // 2MB
    public static final int TARGET_FILE_SIZE = 2 << 20;

    public static final int MAX_GRANDPARENT_OVERLAP_BYTES = 10 * VersionSet.TARGET_FILE_SIZE;

    private final File databaseDir;
    private Version current;
    private LogWriter descriptorFile;
    private final Map<Version, Object> activeVersions = new MapMaker().weakKeys().makeMap();

    private long manifestNumber = 1;
    private final AtomicLong nextFileNumber = new AtomicLong(2);
    private long preLogNumber;
    private long logNumber;
    private long lastSequenceId;

    private final Map<Integer, MemoryTableKey> compactionPointer = new TreeMap<>();

    private final TableCache tableCache;
    private final MemoryTableKeyComparator comparator;

    VersionSet(File databaseDir, TableCache tableCache, MemoryTableKeyComparator memoryTableKeyComparator) {
        this.databaseDir = databaseDir;
        this.tableCache = tableCache;
        this.comparator = memoryTableKeyComparator;

        addVersion(new Version(this));
    }


    public void logAndApply(VersionEdit edit) throws IOException {
        Preconditions.checkState(edit != null, "edit is null");

        if (edit.getPreLogNumber() == null) {
            edit.setPreLogNumber(preLogNumber);
        }

        if (edit.getLogNumber() == null) {
            edit.setLogNumber(logNumber);
        }

        edit.setNextFileNumber(nextFileNumber.get());
        edit.setLastSequence(lastSequenceId);

        Version version = new Version(this);

        // apply edit
        Builder builder = new Builder(this, current);
        builder.apply(edit);
        builder.saveTo(version);
        System.out.println("save to done");
        // make sure compaction
        finalizerVersion(version);

        // todo 0909 write log(version log)
        boolean changeManifest = false;
        try {
            if (descriptorFile == null) {
                descriptorFile = new FileChannelLogWriter(
                        new File(databaseDir, FileNameGenerator.descriptorFile(manifestNumber)), manifestNumber);
                writeSnapshot(descriptorFile);
                changeManifest = true;
            }
            // todo diff
//            edit.setNextFileNumber(nextFileNumber.get());
            Slice record = edit.encode();
            descriptorFile.appendLog(record, true);

            if (changeManifest) {
                FileNameGenerator.setCurrentManifest(databaseDir, FileNameGenerator.descriptorFile(manifestNumber));
            }
        } catch (IOException exception) {
            descriptorFile.close();
            new File(databaseDir, FileNameGenerator.descriptorFile(manifestNumber)).delete();
            descriptorFile = null;
            throw exception;
        }

        // write version
        appendVersion(version);
        preLogNumber = edit.getPreLogNumber();
        logNumber = edit.getLogNumber();


        for (int i = 0; i < current.numberOfLevels(); i++) {
            System.out.println("level == " + i);
            for (FileMeta levelFile : current.getLevelFiles(i)) {
                System.out.println("level == " + i + " file number == " + levelFile.getFileNumber());
            }
        }

    }

    public void writeSnapshot(LogWriter logWriter) throws IOException {
        VersionEdit versionEdit = new VersionEdit();

        for (Entry<Integer, MemoryTableKey> entry : compactionPointer.entrySet()) {
            versionEdit.addCompactionPoint(entry.getKey(), entry.getValue());
        }

        for (int i = 0; i < current.numberOfLevels(); i++) {
            for (FileMeta fileMeta : current.getLevelFiles(i)) {
                versionEdit.addFile(i, fileMeta);
            }
        }

        DynamicSliceOutput output = new DynamicSliceOutput(0);
        for (VersionEditTag editTag : VersionEditTag.values()) {
            editTag.writeEdit(output, versionEdit);
        }

        Slice record = output.slice();
        logWriter.appendLog(record, true);
    }

    public void finalizerVersion(Version version) {
        int bestLevel = 0;
        double bestScore = -1;
        for (int i = 0; i < version.numberOfLevels() - 1; i++) {
            double score;
            if (i == 0) {
                score = 1.0 * version.getLevelFiles(0).size() / Constants.MAX_LEVEL_FILE_COUNT;
            } else {
                score = 1.0 * version.getLevelFilesTotalBytes(i) / getLevelMaxBytes(i);
            }

            if (score > bestScore) {
                bestScore = score;
                bestLevel = i;
            }
        }

        version.setCompactionLevel(bestLevel);
        version.setCompactionScore(bestScore);
    }

    public void appendVersion(Version version) {
        Version preVersion = current;
        // todo fix 1021 save version don't add file  ---------> current = version
        current = version;
        activeVersions.put(version, new Object());
        if (preVersion != null) {
            preVersion.release();
        }
    }

    public void recover() throws IOException {

        File manifest = new File(databaseDir, "CURRENT_MANIFEST");
        Preconditions.checkState(manifest.exists(), "manifest is not exists");
        String currentManifest = Files.toString(manifest, StandardCharsets.UTF_8);
        Preconditions.checkState(currentManifest != null, "manifest content bad");

        Builder builder = new Builder(this, current);

        try (FileChannel fileChannel = new FileInputStream(new File(databaseDir, currentManifest)).getChannel()) {
            Long nextFileNumber = null;
            Long lastSequenceNumber = null;
            Long preLogFileNumber = null;
            Long logNumber = null;

            LogReader logReader = new LogReader(fileChannel, 0, true, new LogMintor());
            Slice record = logReader.readRecord();
            while (record != null) {
                VersionEdit edit = new VersionEdit(record);

                builder.apply(edit);

                nextFileNumber = coalesce(edit.getNextFileNumber(), nextFileNumber);
                lastSequenceNumber = coalesce(edit.getLastSequence(), lastSequenceNumber);
                preLogFileNumber = coalesce(edit.getPreLogNumber(), preLogFileNumber);
                logNumber = coalesce(edit.getLogNumber(), logNumber);

                record = logReader.readRecord();
            }

            ArrayList<String> problems = new ArrayList<>();
            if (nextFileNumber == null) {
                problems.add("next file number is null");
            }

            if (lastSequenceNumber == null) {
                problems.add("last sequence number is null");
            }

            if (logNumber == null) {
                problems.add("log number is null");
            }

            if (problems.size() > 0) {
                throw new RuntimeException(Joiner.on("/n/t").join(problems));
            }

            if (preLogFileNumber == null) {
                preLogFileNumber = 0L;
            }

            Version version = new Version(this);
            builder.saveTo(version);
            finalizerVersion(version);

            this.manifestNumber = nextFileNumber;
            this.nextFileNumber.set(nextFileNumber + 1);
            this.lastSequenceId = lastSequenceNumber;
            this.preLogNumber = preLogFileNumber;
            this.logNumber = logNumber;
        }
    }

    private Long coalesce(Long... values) {
        for (Long v : values) {
            if (v != null) return v;
        }
        return null;
    }

    public LookupResult get(MemoryTableKey lookupKey) {
        return current.get(lookupKey);
    }

    public int getLevelMaxBytes(int level) {
        int bytes = TARGET_FILE_SIZE;
        while (level > 1) {
            bytes *= 10;
            level--;
        }

        return bytes;
    }

    public Compaction pickupCompaction() {
        boolean sizeCompaction = current.getCompactionScore() >= 1.0;

        int level;
        List<FileMeta> levelInputs;
        if (sizeCompaction) {
            level = current.getCompactionLevel();
            levelInputs = new ArrayList<>();

            List<FileMeta> levelFiles = current.getLevelFiles(level);
            for (FileMeta file : levelFiles) {
                if (!compactionPointer.containsKey(level)
                        || comparator.compare(file.getMinKey(), compactionPointer.get(level)) > 0) {
                    levelInputs.add(file);
                    break;
                }
            }
            if (levelInputs.size() == 0) {
                // robin level key
                levelInputs.add(levelFiles.get(0));
            }
        } else {
            return null;
        }

        if (level == 0) {
            // level 0 has key overlap
            // so compaction together
            Entry<MemoryTableKey, MemoryTableKey> range = getRange(levelInputs);
            levelInputs = getOverlappingInputs(0, range.getKey(), range.getValue());
        }
        Compaction compaction = setupOtherInputs(level, levelInputs);

        return compaction;
    }

    public Compaction setupOtherInputs(int level, List<FileMeta> levelInputs) {
        Entry<MemoryTableKey, MemoryTableKey> range = getRange(levelInputs);
        MemoryTableKey minKey = range.getKey();
        MemoryTableKey maxKey = range.getValue();

        List<FileMeta> levelupInputs = getOverlappingInputs(level + 1, minKey, maxKey);
        // levelupInputs modified
        range = getRange(levelInputs, levelupInputs);
        MemoryTableKey allStart = range.getKey();
        MemoryTableKey allEnd = range.getValue();
        // expand level inputs
        if (levelupInputs.size() > 0) {
            List<FileMeta> expandLevelInputs = getOverlappingInputs(level, allStart, allEnd);
            if (expandLevelInputs.size() > levelInputs.size()) {
                // levelInputs modified
                range = getRange(expandLevelInputs);
                MemoryTableKey levelNewStart = range.getKey();
                MemoryTableKey levelNewEnd = range.getValue();

                List<FileMeta> expandLevelupInputs = getOverlappingInputs(level + 1, levelNewStart, levelNewEnd);

                if (expandLevelupInputs.size() == levelupInputs.size()) {
                    minKey = levelNewStart;
                    maxKey = levelNewEnd;

                    levelInputs = expandLevelInputs;
                    levelupInputs = expandLevelupInputs;
                    // levelInputs modified
                    range = getRange(levelInputs, levelupInputs);
                    allStart = range.getKey();
                    allEnd = range.getValue();
                }
            }
        }

        // grandparent inputs
        List<FileMeta> grandparentInputs = ImmutableList.of();
        if (level + 2 < Constants.MAX_LEVEL) {
            grandparentInputs = getOverlappingInputs(level + 2, allStart, allEnd);
        }

        Compaction compaction = new Compaction(
                databaseDir, level, current, levelInputs, levelupInputs, grandparentInputs);

        compactionPointer.put(level, maxKey);
        compaction.getEdit().addCompactionPoint(level, maxKey);
        return compaction;
    }

    @SafeVarargs
    public final Entry<MemoryTableKey, MemoryTableKey> getRange(List<FileMeta>... inputs) {
        MemoryTableKey max = inputs[0].get(0).getMaxKey();
        MemoryTableKey min = inputs[0].get(0).getMinKey();

        for (List<FileMeta> input : inputs) {
            for (FileMeta file : input) {
                max = comparator.compare(max, file.getMaxKey()) >= 0 ? max : file.getMaxKey();
                min = comparator.compare(min, file.getMinKey()) <= 0 ? min : file.getMinKey();
            }
        }

        return Maps.immutableEntry(min, max);
    }

    public List<FileMeta> getOverlappingInputs(int level, MemoryTableKey min, MemoryTableKey max) {
        ArrayList<FileMeta> fileMetas = new ArrayList<>();

        UserComparator comparator = this.comparator.getUserComparatorImp();
        for (FileMeta file : current.getLevelFiles(level)) {
            if (comparator.compare(file.getMinKey().getUserKey(), max.getUserKey()) <= 0
                    && comparator.compare(file.getMaxKey().getUserKey(), min.getUserKey()) >= 0) {
                fileMetas.add(file);
            }
        }

        return fileMetas;
    }

    public MergingIterator getMergingIterator(DBImp.CompactionState compactionState) {

        MergingIterator mergingIterator = new MergingIterator(
                compactionState.getCompaction().getAllLevelIterators(), comparator);

        return mergingIterator;
    }


    private static class Builder {
        private final VersionSet versionSet;
        private final Version baseVersion;
        private final List<LevelState> levels;

        public Builder(VersionSet versionSet, Version baseVersion) {
            this.versionSet = versionSet;
            this.baseVersion = baseVersion;
            levels = new ArrayList<>(baseVersion.numberOfLevels());
            for (int i = 0; i < baseVersion.numberOfLevels(); i++) {
                levels.add(new LevelState(versionSet.comparator));
            }
        }

        public void apply(VersionEdit edit) {

            // delete file
            for (Map.Entry<Integer, Long> entry : edit.getDeleteFiles().entries()) {
                Integer level = entry.getKey();
                Long fileNumber = entry.getValue();
                levels.get(level).deleteFiles.add(fileNumber);
            }

            // new file
            for (Map.Entry<Integer, FileMeta> entry : edit.getNewFiles().entries()) {
                Integer level = entry.getKey();
                FileMeta fileMeta = entry.getValue();
                levels.get(level).deleteFiles.remove(fileMeta.getFileNumber());
                levels.get(level).addFiles.add(fileMeta);
            }
        }

        public void saveTo(Version version) throws IOException {

            for (int i = 0; i < baseVersion.numberOfLevels(); i++) {
                List<FileMeta> baseFiles = baseVersion.getLevelFiles(i);
                SortedSet<FileMeta> addFiles = levels.get(i).addFiles;

                ArrayList<FileMeta> sortedFiles = new ArrayList<>(baseFiles.size() + addFiles.size());

                System.out.println("base version level " + i + " file counts " + baseFiles.size());

                sortedFiles.addAll(baseFiles);
                sortedFiles.addAll(addFiles);
                Collections.sort(sortedFiles, new FileMetaByMinKeyAndFileNumber(versionSet.comparator));


                for (FileMeta file : sortedFiles) {
                    maybeAddFile(i, file, version);
                }
                System.out.println("AddFile to done");
            }
        }

        private void maybeAddFile(int level, FileMeta file, Version version) throws IOException {
            if (levels.get(level).deleteFiles.contains(file.getFileNumber())) {
                // delete
            } else {
                List<FileMeta> levelFiles = version.getLevelFiles(level);
                if (level > 0 && !levelFiles.isEmpty()) {
                    boolean overlap = versionSet.comparator
                            .compare(levelFiles.get(levelFiles.size() - 1).getMaxKey(), file.getMinKey()) >= 0;
                    if (overlap) {
                        System.out.println("overlap " +
                                levelFiles.get(levelFiles.size() - 1).getMaxKey().getSequenceId() + " " + file.getMinKey().getSequenceId());
                        throw new IOException(
                                String.format("major compaction is overlap newFile %s in %d level",
                                        file.getFileNumber(), level));
                    }
                }
            }
            version.addFile(level, file);
        }
    }

    private static class LevelState {
        private final SortedSet<FileMeta> addFiles;
        private final Set<Long> deleteFiles;

        LevelState(MemoryTableKeyComparator comparator) {
            addFiles = new TreeSet<>(new FileMetaByMinKeyAndFileNumber(comparator));
            deleteFiles = new HashSet<>();
        }

        public SortedSet<FileMeta> getAddFiles() {
            return addFiles;
        }

        public Set<Long> getDeleteFiles() {
            return deleteFiles;
        }
    }


    private static class FileMetaByMinKeyAndFileNumber implements Comparator<FileMeta> {
        private final MemoryTableKeyComparator memoryTableKeyComparator;

        FileMetaByMinKeyAndFileNumber(MemoryTableKeyComparator memoryTableKeyComparator) {
            this.memoryTableKeyComparator = memoryTableKeyComparator;
        }

        @Override
        public int compare(FileMeta f1, FileMeta f2) {
            return ComparisonChain
                    .start()
                    .compare(f1.getMinKey(), f2.getMinKey(), memoryTableKeyComparator)
                    .compare(f1.getFileNumber(), f2.getFileNumber())
                    .result();
        }
    }


    public void addVersion(Version version) {
        Preconditions.checkState(version != null, " version is null");
        Preconditions.checkState(current != version, "version is current version");

        Version pre = current;
        current = version;
        activeVersions.put(version, new Object());
        if (pre != null) {
//            pre.release();
        }
    }


    public int countOfFileInLevel(int i) {
        return 0;
    }

    public long getPreLogNumber() {
        return preLogNumber;
    }

    public long getLogNumber() {
        return logNumber;
    }

    public void setLogNumber(long logNumber) {
        this.logNumber = logNumber;
    }

    public long getNextFileNumber() {
        return nextFileNumber.incrementAndGet();
    }

    public long getLastSequenceId() {
        return lastSequenceId;
    }

    public void setLastSequenceId(long lastSequenceId) {
        this.lastSequenceId = lastSequenceId;
    }

    public MemoryTableKeyComparator getComparator() {
        return comparator;
    }

    public boolean needCompaction() {
        return false;
    }

    public Version current() {
        return current;
    }
}