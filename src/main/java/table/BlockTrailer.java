package table;

import utils.Slice;
import utils.SliceInput;
import utils.SliceOutput;
import utils.Slices;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 11:33
 **/
public class BlockTrailer {
    private final int crc;
    private final CompressType compressType;
    public final static int ENCODE_LENGTH = 5;

    public BlockTrailer(int crc32, CompressType compressType) {
        this.crc = crc32;
        this.compressType = compressType;
    }

    public static Slice writeBlockTrailer(BlockTrailer blockTrailer){
        Slice slice = Slices.alloc(ENCODE_LENGTH);
        SliceOutput output = slice.output();
        writeBlockTrailer(output, blockTrailer);

        return output.slice();
    }

    public static void writeBlockTrailer(SliceOutput output, BlockTrailer blockTrailer){
        output.writeInt(blockTrailer.crc);
        output.writeByte(blockTrailer.compressType.getId());
    }

    public static BlockTrailer readBlockTrailer(Slice slice) {
        SliceInput input = slice.input();
        int crc = input.readInt();
        int type = input.readByte();
        return new BlockTrailer(crc, CompressType.getById(type));
    }

    public int getCrc() {
        return crc;
    }

    public CompressType getCompressType() {
        return compressType;
    }
}
