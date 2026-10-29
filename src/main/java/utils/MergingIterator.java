package utils;

import imp.MemoryTableKey;
import imp.MemoryTableKeyComparator;

import java.util.*;

import static java.util.Map.Entry;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 10:16
 **/
public class MergingIterator
        extends AbstractSeekingIterator<MemoryTableKey, Slice>
        implements InternalIterator {

    private final List<? extends InternalIterator> iterators;

    private final MemoryTableKeyComparator comparator;

    // todo 0916 complete MergingIterator
    private final PriorityQueue<ComparableIterator> queue;

    public MergingIterator(List<InternalIterator> iterators, MemoryTableKeyComparator comparator) {
        this.iterators = iterators;
        this.queue = new PriorityQueue<>();
        this.comparator = comparator;
        resetPriorityQueue();
    }

    @Override
    public void seekFirstInternal() {
        for (InternalIterator iterator : iterators) {
            iterator.seekToFirst();
        }
        resetPriorityQueue();
    }

    @Override
    public void seekEntryInternal(MemoryTableKey key) {
        for (InternalIterator iterator : iterators) {
            iterator.seek(key);
        }
        resetPriorityQueue();
    }

    // todo fix 1026 reset queue and comparable iterator
    public void resetPriorityQueue() {
        queue.clear();
        for (int i = 0; i < iterators.size(); i++) {
            InternalIterator iterator = iterators.get(i);
            queue.add(new ComparableIterator(iterator, comparator, i, iterator.next()));
        }
    }

    public Map.Entry<MemoryTableKey, Slice> getNextElement() {
        ComparableIterator iterator = queue.poll();
        Map.Entry<MemoryTableKey, Slice> result = null;
        if (iterator != null) {
            result = iterator.next();
            if (iterator.hasNext()) {
                queue.add(iterator);
            }
        }
        return result;
    }

    public static class ComparableIterator
            implements Iterator<Entry<MemoryTableKey, Slice>>, Comparable<ComparableIterator> {
        private final InternalIterator iterator;
        private final MemoryTableKeyComparator comparator;
        private final int order;

        private Entry<MemoryTableKey, Slice> nextElement;

        public ComparableIterator(
                InternalIterator iterator,
                MemoryTableKeyComparator comparator,
                int order,
                Entry<MemoryTableKey, Slice> nextElement) {
            this.iterator = iterator;
            this.comparator = comparator;
            this.order = order;
            this.nextElement = nextElement;
        }

        public boolean hasNext() {
            return nextElement != null;
        }

        public Entry<MemoryTableKey, Slice> next() {
            Entry<MemoryTableKey, Slice> result = nextElement;
            if (iterator.hasNext()) {
                nextElement = iterator.next();
            } else {
                nextElement = null;
            }
            return result;
        }

        @Override
        public int compareTo(ComparableIterator that) {
            int compare = comparator.compare(this.nextElement.getKey(), that.nextElement.getKey());
            if (compare == 0) {
                return Integer.compare(this.order, that.order);
            }
            return compare;
        }
    }
}
