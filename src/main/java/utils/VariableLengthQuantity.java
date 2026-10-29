package utils;

import java.nio.ByteBuffer;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-03 12:00
 **/
public class VariableLengthQuantity {

    public static void writeVariableLengthInt(SliceOutput out, int length) {
        int flag = 0x80;

        if (length < flag && length >= 0) {
            out.writeByte(length);
        } else if (length >>> 7 < flag && length > 0) {
            out.writeByte(length | flag);
            out.writeByte(length >>> 7);
        } else if (length >>> 14 < flag && length > 0) {
            out.writeByte((byte) (length | flag));
            out.writeByte(length >>> 7 | flag);
            out.writeByte(length >>> 14);
        } else if (length >>> 21 < flag && length > 0) {
            out.writeByte(length | flag);
            out.writeByte(length >>> 7 | flag);
            out.writeByte(length >>> 14 | flag);
            out.writeByte(length >>> 21);
        } else {
            out.writeByte(length);
            out.writeByte(length >>> 7 | flag);
            out.writeByte(length >>> 14 | flag);
            out.writeByte(length >>> 21 | flag);
            out.writeByte(length >>> 28);
        }
    }

    public static void writeVariableLengthLong(SliceOutput out, long length) {
        for (; length >= 0x80; length >>>= 7) {
            out.writeByte((int)(length & 0x7f) | 0x80);
        }
        // todo fix write long ---> write byte
        out.writeByte((int) length);
    }

    public static int readVariableLengthInt(SliceInput input) {
        int result = 0;
        // ((32 + 6) / 7 ) * 8 = 5B , 0 7 14 21 28 -> 1B * 5 = 5B
        for (int i = 0; i <= 28; i += 7) {
            int b = input.readUnsignedByte();
            result |= (b & 0x7f) << i;
            if ((b & 0x80) == 0) return result;
        }
        throw new NumberFormatException("read variable length int height bit error");
    }

    public static int readVariableLengthInt(ByteBuffer input) {
        int result = 0;
        // ((32 + 6) / 7 ) * 8 = 5B , 0 7 14 21 28 -> 1B * 5 = 5B
        for (int i = 0; i <= 28; i += 7) {
            int b = input.get();
            result |= (b & 0x7f) << i;
            if ((b & 0x80) == 0) return result;
        }
        throw new NumberFormatException("read variable length int height bit error");
    }

    public static long readVariableLengthLong(SliceInput input) {
        long result = 0;
        // ((64 + 6) / 7) * 8 = 80bit 10B , 0 7 14 21 ... 63 -> 1B * 10 = 10B
        for (int i = 0; i <= 63; i += 7) {
            long b = input.readUnsignedByte();
            result |= ((b & 0x7f) << i);
            if ((b & 0x80) == 0) return result;
        }
        throw new NumberFormatException("read variable length long height bit error");
    }


    public static int variableLengthByteSize(int length) {
        int size = 1;
        for (; (length & (~0x7f)) != 0; length >>>= 7) {
            size++;
        }
        return size;
    }

    public static int variableLengthByteSize(long length) {
        int size = 1;
        for (; (length & (~0x7f)) != 0; length >>>= 7) {
            size++;
        }
        return size;
    }
}
