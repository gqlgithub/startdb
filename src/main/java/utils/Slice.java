package utils;


import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 20:26
 **/
public class Slice implements Comparable<Slice> {
    private final byte[] data;
    private final int offset;
    private final int length;

    public Slice(byte[] data) {
        this.data = data;
        offset = 0;
        length = data.length;
    }

    public Slice(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public Slice(int length) {
        this.data = new byte[length];
        this.offset = 0;
        this.length = length;
    }

    public static Slice alloc(int length) {
        return new Slice(length);
    }

    public static Slice wrappedBuffer(byte[] data) {
        if (data == null)
            return new Slice(0);
        return new Slice(data);
    }

    public void setByte(int index, int value) {
        data[index + offset] = (byte) value;
    }

    public void setBytes(int index, int si, Slice source, int length) {
        setBytes(index, si + source.offset, source.data, length);
    }

    public void setBytes(int index, int sourceIndex, byte[] source, int length) {
        index += offset;
        System.arraycopy(source, sourceIndex, data, index, length);
    }

    public void setBytes(int index, ByteBuffer byteBuffer) {
        index += offset;
        byteBuffer.get(data, index, byteBuffer.remaining());
    }

    public int setBytes(int index, ScatteringByteChannel channel, int length) throws IOException {
        int readBytes = 0;

        ByteBuffer byteBuffer = ByteBuffer.wrap(data, index + offset, length);

        // todo diff  do-while  netty?
        while (readBytes < length) {
            int currentReadBytes;
            try {
                currentReadBytes = channel.read(byteBuffer);
            } catch (ClosedChannelException e) {
                currentReadBytes = -1;
            }
            if (currentReadBytes < 0) {
                if (readBytes == 0) {
                    return -1;
                }
                break;
            } else if (currentReadBytes == 0) {
                break;
            }
            readBytes += currentReadBytes;
        }

        return readBytes;
    }

    public void setInt(int index, int value) {
        index += offset;
        data[index] = (byte) (value & 0xff);
        data[index + 1] = (byte) ((value >>> 8) & 0xff);
        data[index + 2] = (byte) ((value >>> 16) & 0xff);
        data[index + 3] = (byte) ((value >>> 24) & 0xff);
    }

    public void setLong(int index, long value) {
        index += offset;
        data[index] = (byte) (value & 0xff);
        data[index + 1] = (byte) ((value >>> 8) & 0xff);
        data[index + 2] = (byte) ((value >>> 16) & 0xff);
        data[index + 3] = (byte) ((value >>> 24) & 0xff);
        data[index + 4] = (byte) ((value >>> 32) & 0xff);
        data[index + 5] = (byte) ((value >>> 40) & 0xff);
        data[index + 6] = (byte) ((value >>> 48) & 0xff);
        data[index + 7] = (byte) ((value >>> 56) & 0xff);
    }

    public byte getByte(int index) {
        return data[index + offset];
    }

    public int getInt(int index) {

        // todo fix block seek restart : index ------> index + offset
        // restartPositions = block.slice() -> new slice 是改变 offset 和 length 新 slice 加 offset
        index += offset;

        return (data[index] & 0xff)
                | (data[index + 1] & 0xff) << 8
                | (data[index + 2] & 0xff) << 16
                | (data[index + 3] & 0xff) << 24;
    }

    public long getLong(int index) {
        return ((data[index] & 0xff))
                | (long) (data[index + 1] & 0xff) << 8
                | (long) (data[index + 2] & 0xff) << 16
                | (long) (data[index + 3] & 0xff) << 24
                | (long) (data[index + 4] & 0xff) << 32
                | (long) (data[index + 5] & 0xff) << 40
                | (long) (data[index + 6] & 0xff) << 48
                | (long) (data[index + 7] & 0xff) << 56;
    }

    public SliceInput input() {
        return new SliceInput(this);
    }

    public SliceOutput output() {
        return new SliceOutputBase(this);
    }

    public void getBytes(int index, FileChannel fileChannel, int length) throws IOException {
//        Preconditions.checkPositionIndexes(index, index + length, length);

        index += offset;
        fileChannel.write(ByteBuffer.wrap(data, index, length));
    }

    public void getBytes(int index, ByteBuffer byteBuffer) {
        index += offset;
        byteBuffer.put(data, index, length);
    }

    public Slice slice() {
        return slice(0, length);
    }

    public Slice slice(int i, int len) {
        if (i == 0 & len == length) {
            return this;
        }

        if (i == 0 & len == 0) {
            return Slices.EMPTY_SLICE;
        }

        // todo *fix  builder  (key.slice() to write sharing key)  i -----> i + offset
        return new Slice(data, i + offset, len);
    }

    public int getUnsignedByte(int i) {
        return data[i] & 0xff;
    }

    public Slice copySlice() {
        return copySlice(0, length);
    }

    public Slice copySlice(int index, int length) {
        index += offset;
        byte[] copiedData = new byte[length];
        System.arraycopy(data, index, copiedData, 0, length);
        return new Slice(copiedData);
    }

    public ByteBuffer toByteBuffer() {
        return toByteBuffer(0, length);
    }

    public ByteBuffer toByteBuffer(int i, int length) {
        i += offset;
        return ByteBuffer.wrap(data, i, length).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public int compareTo(Slice that) {
        int minLen = Math.min(this.length, that.length);

        for (int i = 0; i < minLen; i++) {
            int thisByte = 0xff & this.data[this.offset + i];
            int thatByte = 0xff & that.data[that.offset + i];
            if (thisByte != thatByte) {
                return thisByte - thatByte;
            }
        }

        return this.length - that.length;
    }

    public byte[] getRawArray() {
        return data;
    }

    public int getRawOffset() {
        return offset;
    }

    public int length() {
        return length;
    }

    public boolean equals(Object o) {
        if (this == o) return true;

        Slice slice = (Slice) o;
        int length = slice.length;

        if (this.length != length) return false;

        for (int i = 0; i < length; i++) {
            if (data[i + offset] != slice.data[i + slice.offset]) return false;
        }

        return true;
    }

    public String toString(Charset charset) {
        return toString(0, length, charset);
    }

    // todo fix int index, int length
    public String toString(int i, int length, Charset charset) {
        if (length == 0) {
            return "";
        }

        return Slices.decodeString(toByteBuffer(i, length), charset);
    }
}
