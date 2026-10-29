package utils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 22:09
 **/
public abstract class SliceOutput {
    public abstract Slice slice();
    public abstract void writeByte(int value);
    public abstract void writeBytes(Slice value);
    public abstract void writeBytes(Slice value,int index, int length);
    public abstract int writeBytes(ScatteringByteChannel channel, int length) throws IOException;
    public abstract void writeShort(int value);
    public abstract void writeInt(int value);
    public abstract void writeLong(long sequenceBeginId);
    // todo
    public abstract void writeZero(int i, int length);
    public abstract void reset();
    public abstract int writeableBytes();
    public abstract int size();
}
