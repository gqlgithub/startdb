package imp;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 23:53
 **/
public class LookupKey {
    private final MemoryTableKey memoryTableKey;
    private final long lastSequenceId;

    public LookupKey(MemoryTableKey memoryTableKey, long lastSequenceId) {
        this.memoryTableKey = memoryTableKey;
        this.lastSequenceId = lastSequenceId;
    }

    public MemoryTableKey getMemoryTableKey() {
        return memoryTableKey;
    }

    public long getLastSequenceId() {
        return lastSequenceId;
    }
}
