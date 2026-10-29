package imp;

import table.CompressType;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-01 18:02
 **/
public class Options {

    private int memoryBufferSize = 4 << 20;
    private int bockSize = 4 * 1024;

    private int restartPrefixCount = 16;

    private CompressType compressType = CompressType.NO_COMPRESS;

    public int getMemoryBufferSize() {
        return memoryBufferSize;
    }

    public void setMemoryBufferSize(int memoryBufferSize) {
        this.memoryBufferSize = memoryBufferSize;
    }

    public int getBockSize() {
        return bockSize;
    }

    public Options bockSize(int bockSize) {
        this.bockSize = bockSize;
        return this;
    }

    public Options restartPrefixCount(int restartPrefixCount) {
        this.restartPrefixCount = restartPrefixCount;
        return this;
    }

    public int getRestartPrefixCount() {
        return restartPrefixCount;
    }

    public CompressType getCompressType() {
        return compressType;
    }

    public void setCompressType(CompressType compressType) {
        this.compressType = compressType;
    }
}
