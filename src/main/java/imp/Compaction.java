package imp;

import utils.InternalIterator;
import utils.Level0Iterator;
import utils.LevelIterator;
import utils.Slice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-15 8:02
 **/
public class Compaction {

    private final int level;
    private final Version version;
    private final VersionEdit edit;

    private final List<FileMeta> levelInputs;
    private final List<FileMeta> levelupInputs;
    private final List<FileMeta> grandparentInputs;
    private final List<FileMeta>[] levelAndLevelupArray;

    private final List<FileMeta> outputs;
    private final int maxOutputFileSize;
    private final MemoryTableKeyComparator comparator;

    private int grandparentIndex = 0;
    private int grandparentLevelSize = 0;
    private boolean seenKey = false;

    private int[] levelPointer = new int[Constants.MAX_LEVEL];

    private File dir;

    public Compaction(File dir, int level, Version version, List<FileMeta> levelInputs, List<FileMeta> levelupInputs, List<FileMeta> grandparentInputs) {
        this.level = level;
        this.version = version;
        this.edit = new VersionEdit();

        this.levelInputs = levelInputs;
        this.levelupInputs = levelupInputs;
        this.grandparentInputs = grandparentInputs;

        this.outputs = new ArrayList<>();
        this.maxOutputFileSize = version.maxFileSizeInLevel(level);
        this.comparator = version.getMemoryTableKeyComparator();
        this.levelAndLevelupArray = new List[]{levelInputs, levelupInputs};

        this.dir = dir;
    }

    public List<InternalIterator> getAllLevelIterators() {
        ArrayList<InternalIterator> list = new ArrayList<>();
        for (int which = 0; which < 2; which++) {
            if (level + which == 0) {
//                list.add(new Level0Iterator(levelInputs, comparator));
                list.add(new Level0Iterator(dir,levelInputs, comparator));
            } else {
//                list.add(new LevelIterator(levelAndLevelupArray[which], comparator));
                list.add(new LevelIterator(dir,levelAndLevelupArray[which], comparator));
            }
        }
//        System.out.println(list);
        return list;
    }

    public boolean shouldBeRollup(MemoryTableKey key) {

        while (comparator.compare(grandparentInputs.get(grandparentIndex).getMaxKey(), key) < 0) {
            if (seenKey) {
                grandparentLevelSize += grandparentInputs.get(grandparentIndex).getFileLength();
                grandparentIndex++;
            }
        }

        if (grandparentLevelSize >= VersionSet.MAX_GRANDPARENT_OVERLAP_BYTES) {
            grandparentLevelSize = 0;
            return true;
        }

        seenKey = true;
        return false;
    }


    public boolean isBaseLevelForKey(MemoryTableKey key) {
        // key must be grater than last key
        // find start at pointer
        UserComparator userComparator = comparator.getUserComparatorImp();
        Slice userKey = key.getUserKey();
        for (int i = level + 2; i < Constants.MAX_LEVEL; i++) {
            List<FileMeta> levelFiles = version.getLevelFiles(level);
            while (levelPointer[i] < levelFiles.size()) {
                FileMeta file = levelFiles.get(levelPointer[i]);
                if (userComparator.compare(file.getMaxKey().getUserKey(), userKey) >= 0) {
                    if (userComparator.compare(file.getMinKey().getUserKey(), userKey) <= 0) {
                        return false;
                    } else {
                        // next level
                        break;
                    }
                }
            }
            // file at current level pointer maxKey less this key
            levelPointer[i]++;
        }
        return true;
    }

    public void addOutputFile(FileMeta file) {
        outputs.add(file);
    }

    public void deleteInputFiles() {
        for (FileMeta fileMeta : levelInputs) {
            edit.deleteFiles(level, fileMeta);
        }

        for (FileMeta fileMeta : levelupInputs) {
            edit.deleteFiles(level + 1, fileMeta);
        }
    }

    public List<FileMeta> getLevelInputs() {
        return levelInputs;
    }

    public List<FileMeta> getLevelupInputs() {
        return levelupInputs;
    }

    public List<FileMeta>[] getLevelAndLevelupArray() {
        return levelAndLevelupArray;
    }

    public List<FileMeta> getOutputs() {
        return outputs;
    }

    public VersionEdit getEdit() {
        return edit;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxOutputFileSize() {
        return maxOutputFileSize;
    }

    public long getLastVersionNumber() {
        return version.getLastVersionNumber();
    }

    public boolean isTrailerCompaction() {
        if (levelInputs.size() == 1
                && levelupInputs.size() == 0
                && totalSize(grandparentInputs) < VersionSet.MAX_GRANDPARENT_OVERLAP_BYTES) {
            return true;
        }
        return false;
    }

    private int totalSize(List<FileMeta> inputs) {
        int sum = 0;
        for (FileMeta file : inputs) sum += file.getFileLength();
        return sum;
    }
}