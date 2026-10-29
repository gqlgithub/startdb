package imp;

import utils.SizeOf;
import utils.Slice;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-01 17:58
 **/
public class MemTable implements SeekingIterable<MemoryTableKey, Slice> {
    private final ConcurrentSkipListMap<MemoryTableKey, Slice> table;
    private final AtomicLong memoryUsage = new AtomicLong();

    public MemTable(MemoryTableKeyComparator memoryTableKeyComparator) {
        // todo comparator

        table = new ConcurrentSkipListMap<>(memoryTableKeyComparator);
    }

    public void put(Slice key, long sequenceId, Slice value, ValueType valueType) {
        MemoryTableKey memoryTableKey = new MemoryTableKey(key, sequenceId, valueType);
        table.put(memoryTableKey, value);

        memoryUsage.addAndGet(key.length() + SizeOf.LONG + value.length());
    }

    public LookupResult get(MemoryTableKey key) {
        Entry<MemoryTableKey, Slice> entry = table.ceilingEntry(key);
        if (entry != null
                && entry.getKey().getUserKey().equals(key.getUserKey())) {
            if (entry.getKey().getValueType() == ValueType.VALUE) {
                return LookupResult.ok(entry.getKey(), entry.getValue());
            }
            return LookupResult.delete(entry.getKey());
        }

        return null;
    }

    public long memoryUsage() {
        return memoryUsage.get();
    }

    public int size(){
        return table.size();
    }

    @Override
    public Iterator<Entry<MemoryTableKey, Slice>> iterator() {
        return table.entrySet().iterator();
    }

    @Override
    public void forEach(Consumer<? super Entry<MemoryTableKey, Slice>> action) {
        SeekingIterable.super.forEach(action);
    }

    @Override
    public Spliterator<Entry<MemoryTableKey, Slice>> spliterator() {
        return SeekingIterable.super.spliterator();
    }
}
