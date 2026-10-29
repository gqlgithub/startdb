package imp;

import utils.InternalTableIterator;
import table.Table;
import utils.Slice;

import java.util.List;
import java.util.Map.Entry;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 21:08
 **/
public class Level {
    private final int levelNumber;
    private final List<FileMeta> files;
    private final TableCache tableCache;
    private final MemoryTableKeyComparator comparator;

    public Level(int levelNumber, List<FileMeta> files, TableCache tableCache, MemoryTableKeyComparator memoryTableKeyComparator) {
        this.levelNumber = levelNumber;
        this.files = files;
        this.tableCache = tableCache;
        this.comparator = memoryTableKeyComparator;
    }

    public LookupResult get(MemoryTableKey lookupKey) {

        int index = findFile(lookupKey);
        if (index != -1) {
            FileMeta targetFile = files.get(index);
            Table table = tableCache.get(targetFile.getFileNumber());
            InternalTableIterator iterator = new InternalTableIterator(table.iterator());
            iterator.seek(lookupKey);
            if (iterator.hasNext()) {
                Entry<MemoryTableKey, Slice> next = iterator.next();
                if(next.getKey().getValueType() == ValueType.VALUE){
                    return LookupResult.ok(next.getKey(), next.getValue());
                }
                return LookupResult.delete(next.getKey());
            }
        }

        return null;
    }

    public boolean someOverlapInLevel(Slice minKey, Slice maxKey) {
        MemoryTableKey memoryTableKey = new MemoryTableKey(minKey, SequenceNumber.MAX_SEQUENCE_NUMBER, ValueType.VALUE);
        int index = findFile(memoryTableKey);
        if (index != -1
                && comparator.getUserComparatorImp().compare(maxKey, files.get(0).getMinKey().getUserKey()) >= 0) {
            return true;
        }
        return false;
    }

    public int findFile(MemoryTableKey key) {
        int left = 0;
        int right = files.size() - 1;
        int index = -1;
        while (left <= right) {
            int mid = left + ((right - left) >> 1);
            if (comparator.compare(files.get(mid).getMaxKey(), key) < 0) {
                left = mid + 1;
            } else {
                index = right;
                right = mid - 1;
            }
        }

        return index;
    }

    public List<FileMeta> files() {
        return files;
    }
}
