package imp;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 13:31
 **/
public interface SeekingIterable<K,V> extends Iterable<Entry<K,V>>{
    @Override
    Iterator<Entry<K, V>> iterator();
}