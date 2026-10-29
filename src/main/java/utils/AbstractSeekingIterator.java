package utils;

import imp.SeekingIterator;

import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-11 14:33
 **/
public abstract class AbstractSeekingIterator<K, V> implements SeekingIterator<K, V> {

    // todo fix 1024 seeking iterator : seeking peeking nextElement, iterator : hasNext/next
    public Entry<K, V> nextElement;

    @Override
    public void seekToFirst() {
        nextElement = null;
        seekFirstInternal();
    }

    @Override
    public void seek(K key) {
        nextElement = null;
        seekEntryInternal(key);
    }

    @Override
    public Entry<K, V> peek() {
        if (nextElement == null) {
            nextElement = getNextElement();
        }
        return nextElement;
    }

    @Override
    public boolean hasNext() {
        if (nextElement == null) {
            nextElement = getNextElement();
        }
        return nextElement == null;
    }

//    @Override
//    public Entry<K, V> next() {
//        Entry<K, V> result = nextElement;
//        nextElement = getNextElement();
//        return result;
//    }

    @Override
    public Entry<K, V> next() {
        if (nextElement == null) {
            nextElement = getNextElement();
            if (nextElement == null) {
                throw new NoSuchElementException();
            }
        }
        Entry<K, V> result = nextElement;
        nextElement = null;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public abstract void seekFirstInternal();

    public abstract void seekEntryInternal(K key);

    public abstract Entry<K, V> getNextElement();
}
