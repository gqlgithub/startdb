package imp;

import api.WriteBatch;
import com.google.common.collect.Maps;
import utils.Slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import static java.util.Objects.requireNonNull;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-03 10:01
 **/
public class WriteBatchImp implements WriteBatch {
    private List<Entry<Slice, Slice>> batch = new ArrayList<>();
    private int totalByteSize;

    @Override
    public WriteBatchImp put(byte[] key, byte[] value) {
        requireNonNull(key, "key require not null");
        requireNonNull(value, "value require not null");
        batch.add(Maps.immutableEntry(Slice.wrappedBuffer(key), Slice.wrappedBuffer(value)));

        // keyLength 5B valueLength 5B opType 1B
        totalByteSize += 12 + key.length + value.length;
        return this;
    }

    public WriteBatchImp put(Slice key, Slice value) {
        requireNonNull(key, "key require not null");
        requireNonNull(value, "value require not null");
        batch.add(Maps.immutableEntry(key, value));

        // keyLength 5B valueLength 5B opType 1B
        totalByteSize += 12 + key.length() + value.length();
        return this;
    }

    @Override
    public WriteBatch delete(byte[] key) {
        requireNonNull(key, "key is require not null");
        batch.add(Maps.immutableEntry(Slice.wrappedBuffer(key), null));

        // keyLength 5B opType 1B
        totalByteSize += 6 + key.length;
        return this;
    }

    public WriteBatch delete(Slice key) {
        requireNonNull(key, "key is require not null");
        batch.add(Maps.immutableEntry(key, null));

        // keyLength 5B opType 1B
        totalByteSize += 6 + key.length();
        return this;
    }

    public void foreach(Handler handler) {
        for (Entry<Slice, Slice> entry : batch) {
            Slice key = entry.getKey();
            Slice value = entry.getValue();
            if(value != null){
                handler.put(key, value);
            }else {
                handler.delete(key);
            }
        }
    }

    public interface Handler {
        void put(Slice key, Slice value);
        void delete(Slice key);
    }

    public int size() {
        return batch.size();
    }

    public int getTotalByteSize() {
        return totalByteSize;
    }
}
