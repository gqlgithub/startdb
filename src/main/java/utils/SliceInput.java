package utils;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 20:44
 **/
public class SliceInput {
    private final Slice slice;
    private int pos;

    SliceInput(Slice slice) {
        this.slice = slice;
    }

    public int available() {
        return slice.length() - pos;
    }

    public Slice readSlice(int length) {
        Slice read = slice.slice(pos, length);
        pos += length;
        return read;
    }

    public int readByte() {
        if (pos == slice.length()) {
            throw new IndexOutOfBoundsException("read out of slice length");
        }
        return slice.getByte(pos++);
    }

    public int readInt() {
        if (pos == slice.length()) {
            throw new IndexOutOfBoundsException("read out of length");
        }
        int result = slice.getInt(pos);
        pos += 4;
        return result;
    }

    public long readLong() {
        slice.getLong(pos);
        long result = slice.getLong(pos);
        pos += 8;
        return result;
    }

    public int readUnsignedByte() {
//        System.out.println(pos);
        return slice.getByte(pos++) & 0xff;
    }

    public long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    public boolean isReadable() {
        return pos < slice.length();
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }
}
