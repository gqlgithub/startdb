package table;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import imp.*;
import utils.SizeOf;
import utils.Slice;
import utils.SliceInput;
import utils.Slices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.Objects;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-11 14:58
 **/
// todo 0911 open block
public abstract class Table implements SeekingIterable<Slice,Slice> {
    private VersionSet versionSet;
    private long fileNumber;

    private FileMeta fileMeta;

    protected String name;
    protected FileChannel fileChannel;
    protected boolean checksum;
    protected Comparator<Slice> comparator;

    protected Block indexBlock;
    protected BlockHandle metaIndexBlockHandle;


    // todo 0915 table struct
    public Table() {
    }

    // fix
    public Table(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean checkSum) throws IOException {
        Objects.requireNonNull(name, "table name is null");
        Objects.requireNonNull(fileChannel, "table file channel is null");
        Objects.requireNonNull(comparator, "table comparator is null");
        Preconditions.checkArgument(fileChannel.size() >= Footer.ENCODE_LENGTH, "file size must be big than footer");

        this.name = name;
        this.fileChannel = fileChannel;
        this.comparator = comparator;
        this.checksum = checkSum;

        Footer footer = init();
        metaIndexBlockHandle = footer.getMetaIndexBlockHandle();

        indexBlock = readBlock(footer.getIndexBlockHandle());
        System.out.println("indexBlock------ " + indexBlock.size());
    }

    protected abstract Footer init() throws IOException;

//    public Table(File databaseDir, long fileNumber, VersionSet versionSet, UserComparatorImp comparator) {
//        this.fileNumber = fileNumber;
//        this.versionSet = versionSet;
//        this.comparator = comparator;
//        String fileName = FileNameGenerator.tableFileName(fileNumber);
//        try {
//            this.fileChannel = new FileInputStream(
//                    new File(databaseDir, FileNameGenerator.tableFileName(fileNumber))).getChannel();
//
//            FileMeta file = versionSet.current().getFileByNumber(fileNumber);
//            this.fileMeta = file;
//
//            Preconditions.checkState(file != null, "file meta of the file number is null");
//            ByteBuffer byteBuffer = ByteBuffer.allocate(Footer.ENCODE_LENGTH + SizeOf.LONG);
//            FileChannel fileChannel = new FileInputStream(new File(databaseDir, fileName)).getChannel();
//            fileChannel.read(byteBuffer, file.getFileLength() - Footer.ENCODE_LENGTH + SizeOf.LONG);
//
//            byteBuffer.clear();
//            Slice buffer = Slices.copiedBuffer(byteBuffer);
//            Footer footer = Footer.readFooter(buffer);
//
//            metaIndexBlockHandle = footer.getMetaIndexBlockHandle();
//            indexBlock = readBlock(footer.getIndexBlockHandle());
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    public Block openBlock(Slice handle) {
        // handle
        BlockHandle blockHandle = BlockHandle.readBlockHandle(handle.input());

        // read block
        Block block;
        try {
            block = readBlock(blockHandle);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return block;
    }

    public abstract Block readBlock(BlockHandle blockHandle) throws IOException;

    public TableIterator iterator() {
        // todo fix
//        Block block = openBlock(BlockHandle.writeBlockHandle(indexBlockHandle));
        return new TableIterator(this, indexBlock.iterator());
    }

    public long getApproximatedOffsetOf(Slice key) throws IOException {

        BlockIterator iterator = indexBlock.iterator();
        iterator.seek(key);
        if (iterator.hasNext()) {
            BlockHandle blockHandle = BlockHandle.readBlockHandle(iterator.next().getValue().input());
            return blockHandle.getOffset();
        }

        return fileChannel.size() - Footer.ENCODE_LENGTH - SizeOf.LONG;
    }
}
