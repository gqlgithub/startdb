package utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 22:08
 **/
public class SliceOutputBase extends SliceOutput {

    private Slice slice;
    private int size;

    SliceOutputBase(Slice slice) {
        this.slice = slice;
    }

    @Override
    public Slice slice() {
        return slice;
    }

    @Override
    public void writeByte(int value) {
        slice.setByte(size++, value);
    }

    @Override
    public void writeBytes(Slice value) {
        writeBytes(0, value, value.length());
    }

    @Override
    public void writeBytes(Slice value, int index, int length) {
        slice.setBytes(size, index, value, length);
        size += length;
    }

    @Override
    public int writeBytes(ScatteringByteChannel channel, int length) throws IOException {
        int readBytes = slice.setBytes(size, channel, length);
        if (readBytes > 0) {
            size += readBytes;
        }
        return readBytes;
    }

    private void writeBytes(int i, Slice value, int length) {
        slice.setBytes(size, i, value, length);
        size += length;
    }

    @Override
    public void writeShort(int value) {

    }

    @Override
    public void writeInt(int value) {
        slice.setInt(size, value);
        size += 4;
    }

    @Override
    public void writeLong(long value) {
        slice.setLong(size, value);
        size += 8;
    }

    @Override
    public void writeZero(int i, int length) {
        size = i + length;
    }

    @Override
    public void reset() {
        size = 0;
    }

    @Override
    public int writeableBytes() {
        return slice.length() - size;
    }

    public int size() {
        return size;
    }
}
