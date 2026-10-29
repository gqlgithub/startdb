package utils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-05 16:50
 **/
public class DynamicSliceOutput extends SliceOutput {
    private Slice slice;
    private int size;

    public DynamicSliceOutput(Slice slice, int size) {
        this.slice = slice;
        this.size = size;
    }

    public DynamicSliceOutput(int blockSize) {
        slice = new Slice(blockSize);
        size = 0;
    }

    @Override
    public void writeByte(int value) {
        slice = Slices.ensureSize(slice, size + 1);
        slice.setByte(size++, value);
    }

    @Override
    public void writeBytes(Slice value) {
        slice = Slices.ensureSize(slice, size + value.length());
        slice.setBytes(size, 0, value, value.length());
        size += value.length();
    }

    @Override
    public void writeBytes(Slice value, int index, int length) {
        slice = Slices.ensureSize(slice, size + length);
        slice.setBytes(size, index, value, length);
        size += length;
    }

    @Override
    public int writeBytes(ScatteringByteChannel fileChannel, int length) throws IOException {
        slice = Slices.ensureSize(this.slice, size + length);

        int readBytes = slice.setBytes(size, fileChannel, length);
        if (readBytes > 0) {
            size += readBytes;
        }
        return readBytes;
    }

    @Override
    public void writeShort(int value) {
        slice = Slices.ensureSize(slice, size + 2);
        slice.setByte(size, value);
        size += 2;
    }

    @Override
    public void writeInt(int value) {
        // todo fix block builder write restart positions : size + 2 ------> size + 4
        slice = Slices.ensureSize(slice, size + 4);
        slice.setInt(size, value);
        size += 4;
    }

    @Override
    public void writeLong(long value) {
        slice = Slices.ensureSize(slice, size + 8);
        slice.setLong(size, value);
        size += 8;
    }

    @Override
    public void writeZero(int i, int length) {

    }

    public void reset() {
        size = 0;
    }

    @Override
    public int writeableBytes() {
        return slice.length() - size;
    }

    @Override
    public Slice slice() {
        // todo fix length ------> size
        return slice.slice(0, size);
    }

    @Override
    public int size() {
        return size;
    }
}
