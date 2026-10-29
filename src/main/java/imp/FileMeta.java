package imp;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 15:27
 **/
public class FileMeta {
    private long fileNumber;
    private MemoryTableKey minKey;
    private MemoryTableKey maxKey;
    private long fileLength;

    public FileMeta(long fileNumber, MemoryTableKey minKey, MemoryTableKey maxKey, long fileLength) {
        this.fileNumber = fileNumber;
        this.minKey = minKey;
        this.maxKey = maxKey;
        this.fileLength = fileLength;
    }

    public MemoryTableKey getMinKey() {
        return minKey;
    }

    public void setMinKey(MemoryTableKey minKey) {
        this.minKey = minKey;
    }

    public MemoryTableKey getMaxKey() {
        return maxKey;
    }

    public void setMaxKey(MemoryTableKey maxKey) {
        this.maxKey = maxKey;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public long getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(long fileNumber) {
        this.fileNumber = fileNumber;
    }
}
