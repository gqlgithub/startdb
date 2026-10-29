package imp;

import api.DB;
import api.DBException;
import table.BytewiseComparator;
import table.TableBuilder;
import utils.*;

import static com.google.common.base.Preconditions.checkState;
import static imp.Constants.L0_SHUTDOWN_FILES_TRIGGER;
import static imp.Constants.L0_SLOWDOWN_FILES_TRIGGER;
import static imp.FileNameGenerator.FileInfo;
import static imp.FileNameGenerator.FileType;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-01 17:46
 **/
public class DBImp implements DB {
    private final File databaseDir;
    private final Options options;

    private LogWriter log;

    private MemTable memTable;
    private MemTable muteTable;

    private final VersionSet versionSet;

    private final ReentrantLock lock;
    private final Condition backgroundLock;

    private Future<?> backgroundCompaction;
    private ExecutorService compactionExecutor;
    private volatile Throwable backgroundException;
    private ManualCompaction manualCompaction;

    private final MemoryTableKeyComparator memoryTableKeyComparator;

    private final ArrayList<Long> pendingOutputs = new ArrayList<>();

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public DBImp(File dir, Options opt) throws IOException {
        databaseDir = dir;
        options = opt;

        // todo fix 1020 userComparator ----> memory table key comparator
        memoryTableKeyComparator = new MemoryTableKeyComparator(new BytewiseComparator());

        memTable = new MemTable(memoryTableKeyComparator);
        versionSet = new VersionSet(databaseDir, new TableCache(), memoryTableKeyComparator);


        lock = new ReentrantLock();
        backgroundLock = lock.newCondition();

        compactionExecutor = Executors.newSingleThreadExecutor();

        databaseDir.mkdir();

        long logNumber = versionSet.getNextFileNumber();
        log = new FileChannelLogWriter(new File(databaseDir, FileNameGenerator.logFileName(logNumber)), logNumber);

        recoverFromWALog();
    }

    public void recoverFromWALog() {

    }

    @Override
    public void put(byte[] key, byte[] value) {
        put(key, value, new WriteOption());
    }

    @Override
    public void put(byte[] key, byte[] value, WriteOption option) {
        write(new WriteBatchImp().put(key, value), option);
    }

    // todo 0910 get
    public byte[] get(byte[] key) {
        return get(key, new ReadOption());
    }

    public byte[] get(byte[] key, ReadOption readOption) {
        // todo lock

        Slice buffer = Slice.wrappedBuffer(key);
        SnapshotImp snapshot = getSnapshot();
        MemoryTableKey lookupKey = new MemoryTableKey(buffer, snapshot.getLastSequenceId(), ValueType.VALUE);
        // todo lookupKey
//        LookupKey lookup = new LookupKey(lookupKey, snapshot.getLastSequenceId());
        Slice value = null;

        LookupResult entry = memTable.get(lookupKey);
        if (entry != null && !entry.isDeleted()) {
            value = entry.getValue();
        }

        if (value == null && muteTable != null) {
            entry = muteTable.get(lookupKey);
            if (entry != null && !entry.isDeleted()) {
                value = entry.getValue();
            }
        }

        if (value == null) {
            entry = versionSet.get(lookupKey);
            if (entry != null && entry.getValue() != null) {
                value = entry.getValue();
            }
        }

        return value != null ? value.getRawArray() : null;
    }


    public void write(WriteBatchImp updates, WriteOption writeOption) throws DBException {
        lock.lock();
//        System.out.println("------- ");
        try {
            if (updates.size() > 0) {
                makeRoomForWrite(false);

                // todo 0904 task 1 versions sequence
                // todo fix 1021 last not next, --------------> last + 1
                long sequenceBeginId = versionSet.getLastSequenceId() + 1;
                long sequenceLastId = sequenceBeginId + updates.size() - 1;
                versionSet.setLastSequenceId(sequenceLastId);

                // write wal
                Slice logRecord = writeWriteBatch(sequenceBeginId, updates);
                try {
                    log.appendLog(logRecord, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // write memory table
                updates.foreach(new InsertMemoryHandler(memTable, sequenceBeginId));
            }

        } finally {
            lock.unlock();
        }
    }

    public void makeRoomForWrite(boolean force) {
        checkState(lock.isHeldByCurrentThread());

        boolean allowLimit = force;

        while (true) {
            if (allowLimit && versionSet.countOfFileInLevel(0) >= L0_SLOWDOWN_FILES_TRIGGER) {
                try {
                    lock.unlock();
                    Thread.sleep(1000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(exception);
                } finally {
                    lock.lock();
                }
                allowLimit = false;
            } else if (!force && memTable.memoryUsage() <= options.getMemoryBufferSize()) {
                break;
            } else if (muteTable != null) {
                backgroundLock.awaitUninterruptibly();
            } else if (versionSet.countOfFileInLevel(0) >= L0_SHUTDOWN_FILES_TRIGGER) {
                backgroundLock.awaitUninterruptibly();
            } else {
                checkState(versionSet.getPreLogNumber() == 0);

                try {
                    log.close();
                } catch (IOException exception) {
                    throw new RuntimeException("log close exception");
                }

                long logNumber = versionSet.getNextFileNumber();
                try {
                    log = WALog.createLogWriter(
                            new File(databaseDir, FileNameGenerator.logFileName(logNumber)), logNumber);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                muteTable = memTable;
                memTable = new MemTable(memoryTableKeyComparator);

                force = false;

                // todo 0904 task 1 compaction
                maybeScheduleCompaction();
            }
        }
    }

    private void maybeScheduleCompaction() {
        checkState(lock.isHeldByCurrentThread());

        if (backgroundCompaction != null) {

        } else if (shutdown.get()) {

        } else if (muteTable == null && !versionSet.needCompaction() && manualCompaction == null) {

        } else {
            backgroundCompaction = compactionExecutor.submit(() -> {
                try {
                    backgroundCall();
                } catch (Throwable t) {
                    backgroundException = t;
                }
                return null;
            });
        }
    }

    public void backgroundCall() throws IOException {
        lock.lock();

        if (backgroundCompaction == null) return;
        if (!shutdown.get()) {
            try {
                backgroundCompaction();
            } finally {
                backgroundCompaction = null;
            }
        }

        try {
            maybeScheduleCompaction();
        } finally {
            try {
                backgroundLock.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    public void backgroundCompaction() throws IOException {
        checkState(lock.isHeldByCurrentThread());

        // mini compaction
        memoryTableCompaction();

        // todo 0915 major compaction
        Compaction compaction = null;

        if (manualCompaction != null) {
//            compaction =
        } else {
            // todo 0915 pickup compaction
            compaction = versionSet.pickupCompaction();
        }

        if (compaction == null) {

        } else if (manualCompaction == null && compaction.isTrailerCompaction()) {
            checkState(compaction.getLevelInputs().size() == 1);
            FileMeta onlyOneFile = compaction.getLevelInputs().get(0);
            compaction.getEdit().deleteFiles(compaction.getLevel(), onlyOneFile);
            compaction.getEdit().addFile(compaction.getLevel() + 1, onlyOneFile);
            versionSet.logAndApply(compaction.getEdit());
        } else {
            System.out.println("pre do compaction task");
            CompactionState compactionState = new CompactionState(compaction);
            // todo 0917 complete compaction
            doCompactionTask(compactionState);
        }

        if (manualCompaction != null){
            manualCompaction = null;
        }
    }

    private void memoryTableCompaction() throws IOException {
        checkState(lock.isHeldByCurrentThread());

        if (muteTable == null) return;


        VersionEdit versionEdit = new VersionEdit();
        Version versionCurrent = versionSet.current();
        writeLevel0Table(muteTable, versionEdit, versionCurrent);

        // todo 0908 apply edit
        versionEdit.setPreLogNumber(0);
        versionEdit.setLogNumber(log.getFileNumber());

        // apply edit
        versionSet.logAndApply(versionEdit);

        // release muteTable
        muteTable = null;

        // delete obsolete file

        // signal all await compaction condition
        backgroundLock.signalAll();
    }

    public void writeLevel0Table(MemTable muteTable, VersionEdit edit, Version cur) throws IOException {
        checkState(lock.isHeldByCurrentThread());

        long fileNumber = versionSet.getNextFileNumber();

        pendingOutputs.add(fileNumber);
        lock.unlock();
        FileMeta fileMeta;
        try {
            fileMeta = buildTable(muteTable, fileNumber);
        } finally {
            lock.lock();
        }
        pendingOutputs.remove(fileNumber);

        //todo 0907 version add file -----------  ok
        int level = 0;
        if (fileMeta != null && fileMeta.getFileLength() > 0) {
            Slice minKey = fileMeta.getMinKey().getUserKey();
            Slice maxKey = fileMeta.getMaxKey().getUserKey();
            if (cur != null) {
                level = cur.pickLevelMemoryTableOutput(minKey, maxKey);
            }
            // todo
            edit.addFile(level, fileMeta);
        }
    }

    private FileMeta buildTable(MemTable muteTable, long fileNumber) throws IOException {
        File file = new File(databaseDir, FileNameGenerator.tableFileName(fileNumber));
        System.out.println("buildTable");
        try {
            FileChannel fileChannel = new FileOutputStream(file).getChannel();
            System.out.println("buildTable--------- ");
            MemoryTableKey minKey = null;
            MemoryTableKey maxKey = null;
            try {
                TableBuilder tableBuilder = new TableBuilder(
                        options, fileChannel, memoryTableKeyComparator.getUserComparatorImp());
                for (Map.Entry<MemoryTableKey, Slice> entry : muteTable) {
                    MemoryTableKey key = entry.getKey();
                    Slice value = entry.getValue();

                    Slice encodeMemoryKey = key.encode();
                    tableBuilder.add(encodeMemoryKey, value);

                    if (minKey == null) {
                        minKey = key;
                    }
                    maxKey = key;
                }

                tableBuilder.finish();
            } finally {
                try {
                    fileChannel.force(true);
                } finally {
                    fileChannel.close();
                }
            }

            if (minKey == null) return null;
            FileMeta fileMeta = new FileMeta(fileNumber, minKey, maxKey, file.length());
            return fileMeta;
        } catch (Exception exception) {
            file.delete();
            exception.printStackTrace();
            throw exception;
        }
    }

    public void doCompactionTask(CompactionState compactionState) throws IOException {
        System.out.println("do compaction task");
        System.out.println(compactionState.getCompaction().getLevel());

        MergingIterator iterator = versionSet.getMergingIterator(compactionState);

        System.out.println("do compaction task" + iterator);

        long lastVersionNumber = compactionState.compaction.getLastVersionNumber();

        boolean hasKey = false;
        Slice lastKey = null;

        long lastKeySequence = SequenceNumber.MAX_SEQUENCE_NUMBER;
        while (iterator.hasNext() && !shutdown.get()) {

            Entry<MemoryTableKey, Slice> entry = iterator.peek();
            MemoryTableKey key = entry.getKey();

            System.out.println(key.getSequenceId());

            // roll up table bigger than MAX_GRANDPARENT_OVERLAP_BYTES
            if (compactionState.compaction.shouldBeRollup(key) && compactionState.builder != null) {
                finishCompactionFile(compactionState);
            }

            boolean drop = false;
            if (!hasKey || !key.getUserKey().equals(lastKey)) {
                hasKey = true;
                lastKey = key.getUserKey();
                lastKeySequence = SequenceNumber.MAX_SEQUENCE_NUMBER;
            }

            if (lastKeySequence <= lastVersionNumber) {
                drop = true;
            } else if (key.getValueType() == ValueType.DELETION
                    && key.getSequenceId() <= lastVersionNumber
                    && compactionState.compaction.isBaseLevelForKey(key)) {
                drop = true;
            }
            lastKeySequence = key.getSequenceId();

            if (!drop) {
                if (compactionState.builder == null) {
                    openCompactionFile(compactionState);
                }

                if (compactionState.minKey == null) {
                    compactionState.minKey = key;
                }
                compactionState.maxKey = key;
                compactionState.builder.add(key.encode(), entry.getValue());

                if (compactionState.builder.totalSize() >= compactionState.compaction.getMaxOutputFileSize()) {
                    finishCompactionFile(compactionState);
                }
            }

            iterator.next();
        }

        if (compactionState.builder != null) {
            finishCompactionFile(compactionState);
        }

        installCompaction(compactionState);
    }

    public void openCompactionFile(CompactionState compactionState) throws IOException {
        long nextFileNumber = versionSet.getNextFileNumber();

        pendingOutputs.add(nextFileNumber);

        compactionState.fileChannel = new FileOutputStream(
                new File(databaseDir, FileNameGenerator.tableFileName(nextFileNumber))).getChannel();
        compactionState.builder = new TableBuilder(
                options, compactionState.fileChannel, memoryTableKeyComparator.getUserComparatorImp());

        compactionState.fileNumber = nextFileNumber;
        compactionState.minKey = null;
        compactionState.maxKey = null;
        compactionState.currentFileSize = 0;
    }

    public void finishCompactionFile(CompactionState compactionState) throws IOException {

        compactionState.builder.finish();
        compactionState.currentFileSize = compactionState.builder.totalSize();
        compactionState.totalSize += compactionState.currentFileSize;

        FileMeta fileMeta = new FileMeta(
                compactionState.fileNumber,
                compactionState.minKey,
                compactionState.maxKey,
                compactionState.currentFileSize);

        compactionState.compaction.addOutputFile(fileMeta);

        compactionState.builder = null;

        compactionState.fileChannel.force(true);
        compactionState.fileChannel.close();
        compactionState.fileChannel = null;
    }

    public void installCompaction(CompactionState compactionState) throws IOException {

        compactionState.compaction.deleteInputFiles();

        VersionEdit edit = compactionState.compaction.getEdit();
        int level = compactionState.compaction.getLevel();
        List<FileMeta> outputs = compactionState.compaction.getOutputs();
        for (FileMeta file : outputs) {
            edit.addFile(level + 1, file);
            pendingOutputs.remove(file.getFileNumber());
        }

        versionSet.logAndApply(edit);
        deleteObsoleteFile();
    }

    public Slice writeWriteBatch(long sequenceBeginId, WriteBatchImp updates) {
        Slice slice = Slices.alloc(SizeOf.LONG + SizeOf.INT + updates.getTotalByteSize());
        SliceOutput output = slice.output();
        output.writeLong(sequenceBeginId);
        output.writeInt(updates.size());

        updates.foreach(new WriteBatchImp.Handler() {
            @Override
            public void put(Slice key, Slice value) {
                output.writeByte(ValueType.VALUE.getPersistentId());
                Slices.writeVariableLengthAndValue(output, key);
                Slices.writeVariableLengthAndValue(output, value);
            }

            @Override
            public void delete(Slice key) {
                output.writeByte(ValueType.DELETION.getPersistentId());
                Slices.writeVariableLengthAndValue(output, key);
            }
        });

        return slice.slice(0, slice.length());
    }

    private WriteBatchImp readWriteBatch(SliceInput input, int updates) throws IOException {
        int entries = 0;
        WriteBatchImp writeBatchImp = new WriteBatchImp();
        while (input.isReadable()) {
            ValueType type = ValueType.getByPersistentId(input.readByte());
            if (type == ValueType.VALUE) {
                Slice key = Slices.readVariableLengthAndValue(input);
                Slice value = Slices.readVariableLengthAndValue(input);
                writeBatchImp.put(key, value);
            } else if (type == ValueType.DELETION) {
                Slice key = Slices.readVariableLengthAndValue(input);
                writeBatchImp.delete(key);
            } else {
                throw new IllegalStateException("value type is not found");
            }
            entries++;
        }

        if (entries != updates) {
            throw new IOException(
                    String.format("read write-batch entries %d not equals write write-batch updates %d"
                            , entries
                            , updates));
        }

        return writeBatchImp;
    }

    // todo 0920 recover
    public void recover() throws IOException {
        // 1. version -> compaction point, level and files, nextFileNumber, lastSequenceNumber, logNumber
        // 2. wal -> in memory entry, flushing and  no flush manifest entry, update lastSequenceNumber
        // 3. schedule compaction
        // 4. obsolete

        // version recover
        versionSet.recover();

        // wal recover
        // 1. == logNumber
        // 2. >= logNumber
        // >= memory table max size  ->  flush -> nextLogNumber(maxLogNumber) may be loss some logNumber ??

        long lastLogNumber = versionSet.getLogNumber();
        long preLogNumber = versionSet.getPreLogNumber();

        ArrayList<Long> logList = new ArrayList<>();
        List<File> fileList = FileNameGenerator.listFiles(databaseDir);

        for (File fileName : fileList) {
            FileInfo fileInfo = FileNameGenerator.parseFileName(fileName);
            if (fileInfo != null
                    && fileInfo.getType() == FileType.LOG
                    && (fileInfo.getFileNumber() >= lastLogNumber || fileInfo.getFileNumber() == preLogNumber)) {
                logList.add(fileInfo.getFileNumber());
            }
        }

        Collections.sort(logList);
        VersionEdit edit = new VersionEdit();
        for (Long log : logList) {

            long logFileWithinSequenceId = recoverWalFile(log, edit);

            if (logFileWithinSequenceId > versionSet.getLastSequenceId()) {
                versionSet.setLastSequenceId(logFileWithinSequenceId);
            }
        }

        long nextFileNumber = versionSet.getNextFileNumber();
        this.log = new FileChannelLogWriter(new File(FileNameGenerator.logFileName(nextFileNumber)), nextFileNumber);
        versionSet.setLogNumber(nextFileNumber);

        versionSet.logAndApply(edit);

        deleteObsoleteFile();

        maybeScheduleCompaction();
    }

    private long recoverWalFile(Long logNumber, VersionEdit edit) throws IOException {
        long maxSequence = 0;

        try (FileInputStream fileInputStream =
                     new FileInputStream(FileNameGenerator.logFileName(logNumber));
             FileChannel fileChannel = fileInputStream.getChannel()) {

            LogReader logReader = new LogReader(fileChannel, 0, true, new LogMintor());

            for (Slice record = logReader.readRecord(); record != null; record = logReader.readRecord()) {
                SliceInput input = record.input();
                long beginSequenceId = input.readLong();
                int updates = input.readInt();

                WriteBatchImp writeBatchImp = readWriteBatch(input, updates);

                long recordSequence = beginSequenceId + updates - 1;
                maxSequence = Math.max(maxSequence, recordSequence);

                if (memTable == null) {
                    memTable = new MemTable(memoryTableKeyComparator);
                }

                writeBatchImp.foreach(new InsertMemoryHandler(memTable, beginSequenceId));

                if (memTable.memoryUsage() > options.getMemoryBufferSize()) {
                    writeLevel0Table(memTable, edit, null);
                    memTable = null;
                }
            }

            if (memTable != null && memTable.size() > 0) {
                writeLevel0Table(memTable, edit, null);
            }

            return maxSequence;
        }
    }

    public SnapshotImp getSnapshot() {
        lock.lock();
        SnapshotImp snapshotImp = new SnapshotImp(versionSet.current(), versionSet.getLastSequenceId());
        lock.unlock();
        return snapshotImp;
    }

    public void deleteObsoleteFile() {

    }

    public static class InsertMemoryHandler implements WriteBatchImp.Handler {
        private final MemTable memTable;
        private long sequenceId;

        public InsertMemoryHandler(MemTable mem, long seqId) {
            memTable = mem;
            sequenceId = seqId;
        }

        @Override
        public void put(Slice key, Slice value) {
            memTable.put(key, sequenceId++, value, ValueType.VALUE);
        }

        @Override
        public void delete(Slice key) {
            memTable.put(key, sequenceId++, Slices.EMPTY_SLICE, ValueType.DELETION);
        }
    }

    public static class ManualCompaction {

    }

    public static class CompactionState {
        private final Compaction compaction;

        private long fileNumber;
        private MemoryTableKey maxKey;
        private MemoryTableKey minKey;
        private long currentFileSize;

        private FileChannel fileChannel;
        private TableBuilder builder;

        private int totalSize;

        public CompactionState(Compaction compaction) {
            this.compaction = compaction;
        }

        public Compaction getCompaction() {
            return compaction;
        }
    }
}
