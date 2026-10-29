package imp;

import utils.Slice;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 23:02
 **/
public class LookupResult {
    private final MemoryTableKey key;
    private final Slice value;
    private final boolean deleted;

    public LookupResult(MemoryTableKey key, Slice value, boolean deleted) {
        this.key = key;
        this.value = value != null ? value.slice() : null;
        this.deleted = deleted;
    }

    public static LookupResult ok(MemoryTableKey key, Slice value) {
        return new LookupResult(key, value, false);
    }

    public static LookupResult delete(MemoryTableKey key) {
        return new LookupResult(key, null, true);
    }

    public MemoryTableKey getKey() {
        return key;
    }

    public Slice getValue() {
        return value;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
