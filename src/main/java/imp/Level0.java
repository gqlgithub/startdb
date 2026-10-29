package imp;

import utils.InternalTableIterator;
import table.Table;
import utils.Slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 21:08
 **/
public class Level0 {

    private final List<FileMeta> files;
    private final TableCache tableCache;
    private final MemoryTableKeyComparator comparator;

    public Level0(List<FileMeta> files, TableCache tableCache, MemoryTableKeyComparator memoryTableKeyComparator) {
        this.files = files;
        this.tableCache = tableCache;
        this.comparator = memoryTableKeyComparator;
    }

    public LookupResult get(MemoryTableKey key) {

        ArrayList<FileMeta> lookupFiles = new ArrayList<>();
        for (FileMeta file : files) {
            if (comparator.getUserComparatorImp().compare(file.getMinKey().getUserKey(), key.getUserKey()) <= 0
                    && comparator.getUserComparatorImp().compare(file.getMaxKey().getUserKey(), key.getUserKey()) >= 0) {
                lookupFiles.add(file);
            }
        }

        // Collections.sort()

        for (int i = lookupFiles.size() - 1; i >= 0 ; i--) {
            Table table = tableCache.get(lookupFiles.get(i).getFileNumber());
            InternalTableIterator iterator = new InternalTableIterator(table.iterator());
            iterator.seek(key);
            if(iterator.hasNext()){
                Entry<MemoryTableKey, Slice> next = iterator.next();
                if(next.getKey().getValueType() == ValueType.VALUE){
                    return LookupResult.ok(next.getKey(), next.getValue());
                }
                return LookupResult.delete(next.getKey());
            }
        }

        return null;
    }

    // todo diff
    public boolean someOverlapInLevel0(Slice minKey, Slice maxKey) {

//        MemoryTableKey memoryTableKey = new MemoryTableKey(minKey, SequenceNumber.MAX_SEQUENCE_NUMBER, ValueType.VALUE);

        for (FileMeta file : files) {
            if (comparator.getUserComparatorImp().compare(file.getMinKey().getUserKey(), maxKey) <= 0
                    && comparator.getUserComparatorImp().compare(file.getMaxKey().getUserKey(), minKey) >= 0) {
                return true;
            }
        }
        return false;
    }

    public List<FileMeta> files() {
        return files;
    }
}
