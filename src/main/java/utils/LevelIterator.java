package utils;

import imp.*;
import table.BytewiseComparator;
import table.FileChannelTable;
import table.Table;
import table.TableIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 10:06
 **/
public class LevelIterator
        extends AbstractSeekingIterator<MemoryTableKey, Slice>
        implements InternalIterator {

    private final List<FileMeta> files;
    private final TableCache tableCache;
    private final List<InternalTableIterator> iterators;
    private InternalTableIterator iterator;
    private int index;
    private Map.Entry<MemoryTableKey, Slice> nextElement;

    private final MemoryTableKeyComparator comparator;

    public LevelIterator(List<FileMeta> files, MemoryTableKeyComparator comparator, TableCache tableCache) {
        this.files = files;
        this.tableCache = tableCache;
        this.iterators = new ArrayList<>();
        for (FileMeta fileMeta : files) {
            TableIterator iterator = tableCache.get(fileMeta.getFileNumber()).iterator();
            iterators.add(new InternalTableIterator(iterator));
        }
//        this.index = 0;
//        this.iterator = iterators.get(0);
        this.comparator = comparator;
    }

    public LevelIterator(List<FileMeta> files, MemoryTableKeyComparator comparator) {
        this.files = files;
        TableCache tableCache = new TableCache();
        this.tableCache = tableCache;
        this.iterators = new ArrayList<>();
        for (FileMeta fileMeta : files) {
            TableIterator iterator = tableCache.get(fileMeta.getFileNumber()).iterator();
            iterators.add(new InternalTableIterator(iterator));
        }
//        this.index = 0;
//        this.iterator = iterators.get(0);
        this.comparator = comparator;
    }

    public LevelIterator(File databaseDir, List<FileMeta> files, MemoryTableKeyComparator comparator) {
        this.files = files;
        TableCache tableCache = new TableCache();
        this.tableCache = tableCache;
        this.iterators = new ArrayList<>();
        FileChannelTable table = null;
        for (FileMeta file : files) {
            String fileName = FileNameGenerator.tableFileName(file.getFileNumber());
            try {
                table = new FileChannelTable(fileName, new FileInputStream(new File(databaseDir, fileName)).getChannel(), new BytewiseComparator(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            iterators.add(new InternalTableIterator(table.iterator()));
        }
//        this.index = 0;
//        this.iterator = iterators.get(0);
        this.comparator = comparator;

        seekToFirst();
    }

    @Override
    public void seekFirstInternal() {
        index = 0;
        iterator = null;
    }

    @Override
    public void seekEntryInternal(MemoryTableKey key) {

        int left = 0;
        int right = files.size() - 1;
        int mostRightEqualAndSmall = -1;

        while (left <= right) {
            int mid = (left + right) / 2;
            if (comparator.compare(files.get(mid).getMinKey(), key) <= 0) {
                left = mid + 1;
                mostRightEqualAndSmall = mid;
            } else {
                right = mid - 1;
            }
        }

        if (mostRightEqualAndSmall != -1) {
            iterator = tableCache.newIterator(files.get(mostRightEqualAndSmall));
//            iterator = new InternalTableIterator(
//                    tableCache.get(files.get(mostRightEqualAndSmall).getFileNumber()).iterator());
//            for(;iterator.hasNext();iterator.next()){
//                if(comparator.compare(iterator.nextElement.getKey(), key)>=0){
//                    break;
//                }
//            }
            iterator.seek(key);
            index = mostRightEqualAndSmall;
//            if (iterator.hasNext()) {
//                nextElement = iterator.next();
//            } else {
//                nextElement = null;
//            }
        } else {
            iterator = null;
        }
    }

    public Map.Entry<MemoryTableKey, Slice> getNextElement() {
        while (iterator == null || !iterator.hasNext()) {
            if (index <= iterators.size()) {
                iterator = tableCache.newIterator(files.get(index++));
            } else {
                return null;
            }
        }

        return iterator.next();
    }
}