package imp;

import utils.*;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-03 19:35
 **/
public class MemoryTableKey {
    private final Slice userKey;
    private final long sequenceId;
    private final ValueType valueType;

    public MemoryTableKey(Slice userKey, long sequenceId, ValueType valueType) {
        this.userKey = userKey;
        this.sequenceId = sequenceId;
        this.valueType = valueType;
    }

    public Slice getUserKey() {
        return userKey;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    public ValueType getValueType() {
        return valueType;
    }

    // todo fix encode memory key ---------->  encode
    public Slice encode() {
        Slice slice = Slices.alloc(userKey.length() + SizeOf.LONG);
        SliceOutput output = slice.output();
//        encodeMemoryKey(output);
        output.writeBytes(userKey);
        output.writeLong(sequenceId << 8 | valueType.getPersistentId());
        return slice;
    }

//    public void encodeMemoryKey(SliceOutput output) {
//        output.writeBytes(userKey);
//
//        output.writeLong(
//                sequenceId << 8 | valueType.getPersistentId());
//    }

    public static MemoryTableKey decodeMemoryKey(Slice key) {
        return decodeMemoryKey(key.input(), key.length());
    }

    public static MemoryTableKey decodeMemoryKey(SliceInput input, int length) {
        Slice userKey = input.readSlice(length - SizeOf.LONG);
        long l = input.readLong();
        long sequenceId = l >> 1;
        ValueType type = ValueType.getByPersistentId((int) (l & 1));

        return new MemoryTableKey(userKey, sequenceId, type);
    }
}
