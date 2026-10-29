package utils;

import imp.*;
import table.BytewiseComparator;
import table.FileChannelTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 10:03
 **/
// todo 0914 iterator
public class Level0Iterator
        extends AbstractSeekingIterator<MemoryTableKey, Slice>
        implements InternalIterator {

    private final List<InternalTableIterator> inputs;
    private final MemoryTableKeyComparator comparator;
    private final PriorityQueue<ComparableIterator> queue;
    private Map.Entry<MemoryTableKey, Slice> nextElement;

    public Level0Iterator(List<FileMeta> files, MemoryTableKeyComparator comparator, TableCache tableCache) {
        this.inputs = new ArrayList<>();
        for (FileMeta file : files) {
            inputs.add(tableCache.newIterator(file));
        }
        this.comparator = comparator;
        this.queue = new PriorityQueue<>(inputs.size());
//        this.iterator = new ComparableIterator(iterators, comparator);
        resetPrioriQueue(comparator);
    }

    public Level0Iterator(List<FileMeta> files, MemoryTableKeyComparator comparator) {
        this.inputs = new ArrayList<>();
        TableCache tableCache = new TableCache();
        for (FileMeta file : files) {
            inputs.add(tableCache.newIterator(file));
        }
        this.comparator = comparator;
        this.queue = new PriorityQueue<>(inputs.size());
        resetPrioriQueue(comparator);
    }

    public Level0Iterator(File databaseDir, List<FileMeta> files, MemoryTableKeyComparator comparator) {
        this.inputs = new ArrayList<>();

        TableCache tableCache = new TableCache();
        FileChannelTable table = null;
        for (FileMeta file : files) {
            String fileName = FileNameGenerator.tableFileName(file.getFileNumber());
            try {
                table = new FileChannelTable(fileName, new FileInputStream(new File(databaseDir, fileName)).getChannel(), new BytewiseComparator(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputs.add(new InternalTableIterator(table.iterator()));
        }
        this.comparator = comparator;
        this.queue = new PriorityQueue<>(inputs.size());
        seekToFirst();
    }

    @Override
    public void seekFirstInternal() {
        for (InternalTableIterator input : inputs) {
            input.seekToFirst();
        }

        resetPrioriQueue(comparator);
    }

    @Override
    public void seekEntryInternal(MemoryTableKey key) {
        for (InternalTableIterator input : inputs) {
            input.seek(key);
        }

        resetPrioriQueue(comparator);
    }

    @Override
    public Entry<MemoryTableKey, Slice> getNextElement() {
        ComparableIterator iterator = queue.poll();
        if (iterator != null) {
            nextElement = iterator.next();
            if (iterator.hasNext()) {
                queue.add(iterator);
            }
        } else {
            return null;
        }

        return nextElement;
    }

    private void resetPrioriQueue(MemoryTableKeyComparator comparator) {
        queue.clear();
        int index = 0;
        for (InternalTableIterator iterator : inputs) {
            ComparableIterator comparableIterator =
                    new ComparableIterator(index++, iterator, comparator, iterator.next());
            queue.add(comparableIterator);
        }
    }

    public static class ComparableIterator
            implements Iterator<Entry<MemoryTableKey, Slice>>, Comparable<ComparableIterator> {
        private int ordinal;
        private final SeekingIterator<MemoryTableKey, Slice> iterator;
        private Entry<MemoryTableKey, Slice> nextElement;
        private final MemoryTableKeyComparator comparator;

        public ComparableIterator(int ordinal, SeekingIterator<MemoryTableKey, Slice> iterator,
                                  MemoryTableKeyComparator comparator, Entry<MemoryTableKey, Slice> nextElement) {
            this.ordinal = ordinal;
            this.iterator = iterator;
            this.nextElement = nextElement;
            this.comparator = comparator;
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public Entry<MemoryTableKey, Slice> next() {
            if (nextElement == null) {
                throw new NoSuchElementException();
            }

            Entry<MemoryTableKey, Slice> result = nextElement;
            nextElement = iterator.next();
            return result;
        }

        @Override
        public int compareTo(ComparableIterator that) {
            return comparator.compare(this.nextElement.getKey(), that.nextElement.getKey());
        }
    }
}
