package imp;

import com.google.common.collect.PeekingIterator;

import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-10 17:34
 **/
public interface SeekingIterator<K, V> extends PeekingIterator<Map.Entry<K, V>> {

    void seekToFirst();

    void seek(K key);
}
