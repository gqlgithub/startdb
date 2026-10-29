package table;

import com.google.common.base.Preconditions;
import utils.Slice;

import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-11 16:48
 **/
public class BlockEntry implements Map.Entry<Slice, Slice> {
    private final Slice key;
    private final Slice value;

    public BlockEntry(Slice key, Slice value) {
        Preconditions.checkState(key != null, "block entry key is null");
        Preconditions.checkState(value != null, "block entry value is null");
        this.key = key;
        this.value = value;
    }

    @Override
    public Slice getKey() {
        return key;
    }

    @Override
    public Slice getValue() {
        return value;
    }

    @Override
    public Slice setValue(Slice slice) {
        throw  new UnsupportedOperationException("block entry set value");
    }

    @Override
    public boolean equals(Object o){

        BlockEntry entry = (BlockEntry) o;

        if (!key.equals(entry.getKey())){
            return false;
        }

        if(!value.equals(entry.getValue())){
            return false;
        }

        return true;
    }
}
