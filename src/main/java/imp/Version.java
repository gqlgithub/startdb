package imp;

import com.google.common.collect.ImmutableList;
import utils.Slice;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 15:22
 **/
public class Version {

    private final VersionSet versionSet;
    private final Level0 level0;
    private final List<Level> levels;

    private double compactionScore;
    private int compactionLevel;

    public Version(VersionSet versionSet) {
        this.versionSet = versionSet;
        this.level0 = new Level0(new ArrayList<>(), new TableCache(), getMemoryTableKeyComparator());
        ImmutableList.Builder<Level> builder = ImmutableList.builder();
        for (int i = 1; i < Constants.MAX_LEVEL; i++) {
            builder.add(new Level(i, new ArrayList<>(), new TableCache(), getMemoryTableKeyComparator()));
        }
        levels = builder.build();
    }

    public int pickLevelMemoryTableOutput(Slice minKey, Slice maxKey) {
        int level = 0;
        if (!overlapInLevel(0, minKey, maxKey)) {
            while (level < Constants.MAX_MEMORY_OVERLAP_LEVEL) {
                if (overlapInLevel(level + 1, minKey, maxKey)) {
                    break;
                }
                // todo
//                if(){}
                level++;
            }
        }
        return level;
    }

    public boolean overlapInLevel(int level, Slice minKey, Slice maxKey) {
        if (level == 0) {
            return level0.someOverlapInLevel0(minKey, maxKey);
        }
        return levels.get(level - 1).someOverlapInLevel(minKey, maxKey);
    }

    public LookupResult get(MemoryTableKey key) {
        LookupResult entry = level0.get(key);
        if (entry == null) {
            for (Level level : levels) {
                entry = level.get(key);
                if (entry != null) {
                    return entry;
                }
            }
        }

        return null;
    }

    public FileMeta getFileByNumber(long fileNumber) {
        List<FileMeta> files = level0.files();
        int index = binarySearchByNumber(files, fileNumber);
        if (index != -1) {
            return files.get(index);
        }

        for (Level level : levels) {
            files = level.files();
            index = binarySearchByNumber(level.files(), fileNumber);
            if (index != -1) {
                return files.get(index);
            }
        }

        return null;
    }

    public int binarySearchByNumber(List<FileMeta> files, long fileNumber) {
        int l = 0;
        int r = files.size() - 1;
        while (l <= r) {
            int m = (l + r) / 2;
            long number = files.get(m).getFileNumber();
            if (number < fileNumber) {
                l = m + 1;
            } else if (number > fileNumber) {
                r = m - 1;
            } else {
                return m;
            }
        }

        return -1;
    }

    // todo
    public void release() {

    }

    public MemoryTableKeyComparator getMemoryTableKeyComparator() {
        return versionSet.getComparator();
    }

    public int numberOfLevels() {
        return levels.size() + 1;
    }

    public void addFile(int level, FileMeta fileMeta) {
        System.out.println("add file " + "level " + level + " FileNumber " + fileMeta.getFileNumber());
        if (level == 0) {
            level0.files().add(fileMeta);
        } else {
            levels.get(level - 1).files().add(fileMeta);
        }
    }

    public List<FileMeta> getLevelFiles(int level) {
        if (level == 0) {
            return level0.files();
        }
        return levels.get(level - 1).files();
    }

    public int getLevelFilesTotalBytes(int level) {
        int total = 0;
        for (FileMeta file : getLevelFiles(level - 1)) {
            total += file.getFileLength();
        }

        return total;
    }

    public int maxFileSizeInLevel(int level) {
        return VersionSet.TARGET_FILE_SIZE;
    }

    public long getLastVersionNumber() {
        return versionSet.getLastSequenceId();
    }

    public Level0 getLevel0() {
        return level0;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public double getCompactionScore() {
        return compactionScore;
    }

    public void setCompactionScore(double score) {
        compactionScore = score;
    }

    public void setCompactionLevel(int level) {
        compactionLevel = level;
    }

    public int getCompactionLevel() {
        return compactionLevel;
    }
}
