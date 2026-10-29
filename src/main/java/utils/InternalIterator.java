package utils;

import imp.MemoryTableKey;
import imp.SeekingIterator;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 10:10
 **/
public interface InternalIterator
        extends SeekingIterator<MemoryTableKey, Slice>{
}
