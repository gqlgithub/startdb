package table;

import com.google.common.base.Preconditions;
import utils.*;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 18:37
 **/
public class BlockHandle {
    public static final int MAX_ENCODE_LENGTH = 10 + 10;

    private long offset;
    private int size;

    public BlockHandle(long offset, int size) {
        this.offset = offset;
        this.size = size;
    }

    public static Slice writeBlockHandle(BlockHandle blockHandle) {
        Slice slice = Slices.alloc(MAX_ENCODE_LENGTH);
        writeBlockHandle(slice.output(), blockHandle);
        return slice;
    }

    public static void writeBlockHandle(SliceOutput output, BlockHandle blockHandle) {
        VariableLengthQuantity.writeVariableLengthLong(output, blockHandle.offset);
        VariableLengthQuantity.writeVariableLengthLong(output, blockHandle.size);
    }

    public static BlockHandle readBlockHandle(SliceInput input) {
//        System.out.println("input " + input.getPos());
        long offset = VariableLengthQuantity.readVariableLengthLong(input);
        long length = VariableLengthQuantity.readVariableLengthLong(input);
//        System.out.println(offset);
//        System.out.println(length);
        if (length > Integer.MAX_VALUE) throw new IllegalArgumentException("block over int max value");
        return new BlockHandle(offset, (int) length);
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
