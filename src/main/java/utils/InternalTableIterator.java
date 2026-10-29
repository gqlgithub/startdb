package utils;

import com.google.common.collect.Maps;
import imp.MemoryTableKey;
import imp.SeekingIterator;
import table.TableIterator;

import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-11 14:32
 **/
public class InternalTableIterator
        extends AbstractSeekingIterator<MemoryTableKey, Slice>
        implements SeekingIterator<MemoryTableKey, Slice>, InternalIterator {

    private final TableIterator tableIterator;

    public InternalTableIterator(TableIterator iterator) {
        tableIterator = iterator;
    }

    @Override
    public void seekFirstInternal() {
        tableIterator.seekToFirst();
    }

    @Override
    public void seekEntryInternal(MemoryTableKey key) {
        tableIterator.seek(key.encode());
    }

    @Override
    public Map.Entry<MemoryTableKey, Slice> getNextElement() {
        Map.Entry<Slice, Slice> nextElement = tableIterator.getNextElement();
        MemoryTableKey memoryTableKey = MemoryTableKey.decodeMemoryKey(nextElement.getKey());
        return Maps.immutableEntry(memoryTableKey,nextElement.getValue());
    }
}