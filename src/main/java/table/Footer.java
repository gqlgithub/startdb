package table;

import utils.*;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 12:39
 **/
public class Footer {
    // magic number 8B
    public static final int ENCODE_LENGTH = BlockHandle.MAX_ENCODE_LENGTH * 2 + 8;

    private final BlockHandle metaIndexBlockHandle;
    private final BlockHandle indexBlockHandle;

    public Footer(BlockHandle metaIndexBlockHandle, BlockHandle indexBlockHandle) {
        this.metaIndexBlockHandle = metaIndexBlockHandle;
        this.indexBlockHandle = indexBlockHandle;
    }

    public static Slice writeFooter(Footer footer) {
        Slice slice = Slices.alloc(ENCODE_LENGTH + SizeOf.LONG);
        writeFooter(slice.output(), footer);
        return slice;
    }

    public static void writeFooter(SliceOutput output, Footer footer) {
        BlockHandle.writeBlockHandle(output, footer.metaIndexBlockHandle);
        BlockHandle.writeBlockHandle(output, footer.indexBlockHandle);

        // padding 0 to 48B
        output.writeZero(ENCODE_LENGTH - output.size(),output.size());

        // write magic number
        output.writeInt((int) TableBuilder.MAGIC_NUMBER);
        output.writeInt((int) (TableBuilder.MAGIC_NUMBER >> 32));
    }

    public static Footer readFooter(Slice footer) {
        return readFooter(footer.input(), footer.length());
    }

    public static Footer readFooter(SliceInput input, int length) {
        Slice footer = input.readSlice(Footer.ENCODE_LENGTH);

        // todo fix 1014 index block seek first error: footer position don't move,  slice -------> slice input : readBlockHandle(Slice) -> readBlockHandle(SliceInput)
        SliceInput footerInput = footer.input();
        BlockHandle blockMetaHandle = BlockHandle.readBlockHandle(footerInput);
        BlockHandle blockIndexHandle = BlockHandle.readBlockHandle(footerInput);

        long magicNumber = input.readUnsignedInt() | input.readUnsignedInt() << 32;

        if (magicNumber != TableBuilder.MAGIC_NUMBER) {
            throw new IllegalArgumentException("table magic number error");
        }

//
//        System.out.println("read footer");
//        System.out.println(blockMetaHandle.getOffset());
//        System.out.println(blockMetaHandle.getSize());
//        System.out.println(blockIndexHandle.getOffset());
//        System.out.println(blockIndexHandle.getSize());
        return new Footer(blockMetaHandle, blockIndexHandle);
    }

    public BlockHandle getMetaIndexBlockHandle() {
        return metaIndexBlockHandle;
    }

    public BlockHandle getIndexBlockHandle() {
        return indexBlockHandle;
    }
}
