package table;

import utils.SizeOf;
import utils.Slice;
import utils.Slices;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 10:25
 **/
public class FileChannelTable extends Table {
    public FileChannelTable(){}

    public FileChannelTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean checkSum) throws IOException {
        super(name, fileChannel, comparator, checkSum);
    }

    @Override
    protected Footer init() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Footer.ENCODE_LENGTH + SizeOf.LONG);
        fileChannel.read(byteBuffer, fileChannel.size() - Footer.ENCODE_LENGTH - SizeOf.LONG);

        byteBuffer.clear();
        Slice buffer = Slices.copiedBuffer(byteBuffer);
        return  Footer.readFooter(buffer);
    }

    @Override
    public Block readBlock(BlockHandle blockHandle) throws IOException {
        ByteBuffer trailerBuffer = read(
                blockHandle.getOffset() + blockHandle.getSize(), BlockTrailer.ENCODE_LENGTH);
        BlockTrailer blockTrailer = BlockTrailer.readBlockTrailer(Slices.copiedBuffer(trailerBuffer));

        ByteBuffer dataBuffer = read(blockHandle.getOffset(), blockHandle.getSize());
        System.out.println(blockHandle.getOffset() +" " + blockHandle.getSize());
        Slice data = Slices.copiedBuffer(dataBuffer);

        if (blockTrailer.getCompressType() != CompressType.NO_COMPRESS) {
            // uncompress
        }

        for (int i = 0; i < data.length(); i++) {
            System.out.println(data.getByte(i));
        }
        System.out.println("-----");
        return new Block(data, comparator);
    }

    public ByteBuffer read(long offset, int size) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        fileChannel.read(byteBuffer, offset);

        byteBuffer.clear();
        return byteBuffer;
    }
}
