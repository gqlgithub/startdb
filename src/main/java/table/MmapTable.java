package table;

import utils.SizeOf;
import utils.Slice;
import utils.Slices;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-24 10:54
 **/
public class MmapTable extends Table {

    private MappedByteBuffer mappedByteBuffer;

    public MmapTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean checksum) throws IOException {
        super(name, fileChannel, comparator, checksum);
    }

    public Footer init() throws IOException {
        long size = fileChannel.size();
        this.mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        Slice footer = Slices.copiedBuffer(
                mappedByteBuffer, (int) size - Footer.ENCODE_LENGTH - SizeOf.LONG, Footer.ENCODE_LENGTH + SizeOf.LONG);
        return Footer.readFooter(footer);
    }

    @Override
    public Block readBlock(BlockHandle blockHandle) throws IOException {

        BlockTrailer blockTrailer = BlockTrailer.readBlockTrailer(
                Slices.copiedBuffer(
                        mappedByteBuffer,
                        (int) blockHandle.getOffset() + blockHandle.getSize(),
                        BlockTrailer.ENCODE_LENGTH));

        int crc = blockTrailer.getCrc();

        // todo crc check sum

        ByteBuffer byteBuffer = read((int) blockHandle.getOffset(), blockHandle.getSize());

        Slice blockData = null;
        if (blockTrailer.getCompressType() == CompressType.NO_COMPRESS) {
            blockData = Slices.copiedBuffer(byteBuffer);
        } else {
            //
        }

        return new Block(blockData, comparator);
    }

    public ByteBuffer read(int offset, int length) {
        int newPosition = mappedByteBuffer.position() + offset;
        ByteBuffer byteBuffer = mappedByteBuffer.duplicate()
                .order(ByteOrder.LITTLE_ENDIAN)
                .clear()
                .position(newPosition)
                .limit(newPosition + length);

        return byteBuffer;
    }
}
