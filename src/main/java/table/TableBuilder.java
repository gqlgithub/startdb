package table;

import com.google.common.base.Preconditions;
import imp.Options;
import imp.UserComparator;
import imp.VersionSet;
import utils.Slice;
import utils.Slices;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 16:03
 **/
public class TableBuilder {
    // leveldb magic number
    public static long MAGIC_NUMBER = 0xdb4775248b80fb57L;

    private final FileChannel fileChannel;
    private final UserComparator comparator;

    private final int blockSize;
    private final int restartPrefixCount;
    private final BlockBuilder blockBuilder;

    private final BlockBuilder indexBlockBuilder;

    private long position;

    private BlockHandle indexBlockHandle;
    private boolean indexEntryPadding;

    private final CompressType compressType;

    private int entrySize;

    private boolean closed;
    private Slice lastKey;

    public TableBuilder(Options opt, FileChannel channel, UserComparator comparator) {
        Objects.requireNonNull(opt, "table builder options is null");
        Objects.requireNonNull(channel, "table builder channel is null");

        fileChannel = channel;
        this.comparator = comparator;

        blockSize = opt.getBockSize();
        restartPrefixCount = opt.getRestartPrefixCount();
        blockBuilder = new BlockBuilder(
                Math.min((int) (blockSize * 1.1), VersionSet.TARGET_FILE_SIZE), restartPrefixCount, this.comparator);

        // 2M / 4K = 512 -> * 2
        int expectBlockCount = 1024;
        // 10 + 10  ~=  offset(3B) + length(2B) + key(8B) + restartPosition(4B)
        indexBlockBuilder = new BlockBuilder(
                BlockHandle.MAX_ENCODE_LENGTH * expectBlockCount, 1, this.comparator);

        lastKey = Slices.EMPTY_SLICE;

        compressType = opt.getCompressType();
        entrySize = 0;
        closed = false;
    }

    public void add(Slice key, Slice value) throws IOException {
        Objects.requireNonNull(key, "table builder key is null");
        Objects.requireNonNull(key, "table builder value is null");

        Preconditions.checkState(!closed, "add entry to table but is closed");

//        System.out.println(key.toString(StandardCharsets.UTF_8));

        // index block
        if (indexEntryPadding) {
            Preconditions.checkState(blockBuilder.isEmpty(), "add index block but block builder is not empty");

            Slice shortestSeparator = comparator.findShortestSeparator(lastKey, key);
            indexBlockBuilder.add(shortestSeparator, BlockHandle.writeBlockHandle(indexBlockHandle));
            indexEntryPadding = false;
        }

        entrySize++;
        lastKey = key;
        // data block
        blockBuilder.add(key, value);

        int currentSize = blockBuilder.computeCurrentSize();
        if (currentSize > blockSize) {
            flush();
        }
    }

    public void flush() throws IOException {
//        System.out.println("flush---------------- ");
        Preconditions.checkState(!closed, "table closed");
        if (blockBuilder.isEmpty()) {
            return;
        }

        Preconditions.checkState(!indexEntryPadding, "already entry padding");

//        System.out.println("blockHandle writeBlock");
        indexBlockHandle = writeBlock(blockBuilder);
        indexEntryPadding = true;
    }

    public BlockHandle writeBlock(BlockBuilder blockBuilder) throws IOException {
        Slice block = blockBuilder.finish();

//        System.out.println("write---------- ");
//        System.out.println("---------- ");
//        for (int i = 0; i < block.length(); i++) {
//            System.out.println(block.getByte(i));
//        }
//        System.out.println("position "+position);
//        System.out.println("length "+block.length());
//        System.out.println("---------- ");

        BlockTrailer blockTrailer = new BlockTrailer(crc32(compressType, block.length()), compressType);
        Slice trailer = BlockTrailer.writeBlockTrailer(blockTrailer);

        BlockHandle blockHandle = new BlockHandle(position, block.length());

        position += fileChannel.write(new ByteBuffer[]{block.toByteBuffer(), trailer.toByteBuffer()});

        blockBuilder.reset();
        return blockHandle;
    }

    public void finish() throws IOException {
        Preconditions.checkState(!closed, "table is closed");

        flush();

        closed = true;

        if(indexEntryPadding){
            Slice shortestSuccessor = comparator.findShortestSuccessor(lastKey);
            indexBlockBuilder.add(shortestSuccessor, BlockHandle.writeBlockHandle(indexBlockHandle));
        }

        BlockBuilder metaIndexBlockBuilder = new BlockBuilder(256, restartPrefixCount, comparator);
//        System.out.println("metaIndexBlockBuilder writeBlock");
        BlockHandle metaIndexBlockHandle = writeBlock(metaIndexBlockBuilder);

//        System.out.println("indexBlockBuilder writeBlock");
        BlockHandle indexBlockHandle = writeBlock(indexBlockBuilder);
//        System.out.println("w " + indexBlockHandle.getOffset());
//        System.out.println("w " + indexBlockHandle.getSize());

        // 48B footer
        Footer footer = new Footer(metaIndexBlockHandle, indexBlockHandle);
//        System.out.println("write footer");
//        System.out.println(footer.getMetaIndexBlockHandle().getOffset());
//        System.out.println(footer.getMetaIndexBlockHandle().getSize());
//        System.out.println(footer.getIndexBlockHandle().getOffset());
//        System.out.println(footer.getIndexBlockHandle().getSize());

        Slice encodingFooter = Footer.writeFooter(footer);
        fileChannel.write(encodingFooter.toByteBuffer());
    }

    private int crc32(CompressType compressType, int length) {

        return 0;
    }

    public int getEntrySize() {
        return entrySize;
    }

    public long totalSize(){
        return position + Footer.ENCODE_LENGTH;
    }
}
