package table;

import com.google.common.base.Preconditions;
import imp.Options;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.Closeables;
import utils.Slice;
import utils.Slices;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;
import static java.util.Arrays.asList;

/**
 * @Description:
 * @Author:
 * @Date: 2026-10-12 15:58 10-15 18:00
 **/
public abstract class TableTest {
    File file;
    RandomAccessFile randomAccessFile;
    FileChannel fileChannel;

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyFile() throws IOException {
        createTable(file.getAbsolutePath(), fileChannel, new BytewiseComparator(), true);
    }

    @Test
    public void testEmptyBlock() throws IOException {
        testTable(Integer.MAX_VALUE , Integer.MAX_VALUE);
    }

    @Test
    public void testSingleEntrySingleBlock() throws IOException {
        testTable(Integer.MAX_VALUE,Integer.MAX_VALUE, BlockHelper.createBlockEntry("leveldb","kv"));
    }

    @Test
    public void testMultipleEntrySingleBlock() throws IOException {
        List<BlockEntry> blockEntries = asList(
                BlockHelper.createBlockEntry("db/hbase", "kv"),
                BlockHelper.createBlockEntry("db/leveldb", "kv"),
                BlockHelper.createBlockEntry("db/rocksdb", "kv"),
                BlockHelper.createBlockEntry("hbase", "kv"),
                BlockHelper.createBlockEntry("leveldb", "kv"),
                BlockHelper.createBlockEntry("rocksdb", "kv"));

        testTable(Integer.MAX_VALUE, Integer.MAX_VALUE,blockEntries);

        for (int i = 0; i < blockEntries.size(); i++) {
            testTable(Integer.MAX_VALUE, Integer.MAX_VALUE, blockEntries.subList(i, blockEntries.size()));
        }
    }

    @Test
    public void testMultipleEntryMultipleBlock() throws IOException {
        List<BlockEntry> blockEntries = asList(
                BlockHelper.createBlockEntry("db/hbase", "kv"),
                BlockHelper.createBlockEntry("db/leveldb", "kv"),
                BlockHelper.createBlockEntry("db/rocksdb", "kv"),
                BlockHelper.createBlockEntry("hbase", "kv"),
                BlockHelper.createBlockEntry("leveldb", "kv"),
                BlockHelper.createBlockEntry("rocksdb", "kv"));

        testTable(1, Integer.MAX_VALUE,blockEntries);

        testTable(BlockHelper.estimateBlockSize(Integer.MAX_VALUE, blockEntries) / 5, Integer.MAX_VALUE, blockEntries);
    }

    public void testTable(int blockSize, int restartCnt, BlockEntry... entries) throws IOException {
        testTable(blockSize, restartCnt, asList(entries));
    }

    public void testTable(int blockSize, int restartCnt, List<BlockEntry> entryList) throws IOException {
        openTable();
        Options options = new Options()
                .bockSize(blockSize)
                .restartPrefixCount(restartCnt);

        TableBuilder tableBuilder = new TableBuilder(options, fileChannel, new BytewiseComparator());
        for(BlockEntry entry : entryList){
            tableBuilder.add(entry.getKey(), entry.getValue());
        }

        tableBuilder.finish();

        Table table = createTable(file.getAbsolutePath(), fileChannel, new BytewiseComparator(), true);
        TableIterator iterator = table.iterator();
        BlockHelper.assertSequence(iterator, entryList);

        iterator.seekToFirst();
        BlockHelper.assertSequence(iterator, entryList);
        long lastApproximatedOffset = 0;
        for (int i = 0; i < entryList.size(); i++) {
            iterator.seek(entryList.get(i).getKey());
            BlockHelper.assertSequence(iterator, entryList.subList(i, entryList.size()));

            Slice before = BlockHelper.before(entryList.get(i));
            iterator.seek(before);
            BlockHelper.assertSequence(iterator, entryList.subList(i, entryList.size()));

            Slice after = BlockHelper.after(entryList.get(i));
            iterator.seek(after);
            BlockHelper.assertSequence(iterator, entryList.subList(i + 1, entryList.size()));

            long approximatedOffset = table.getApproximatedOffsetOf(entryList.get(i).getKey());

            Assert.assertTrue(approximatedOffset >= lastApproximatedOffset);
            lastApproximatedOffset = approximatedOffset;
        }

        // make max key
        Slice slice = Slices.wrapBuffer(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
        iterator.seek(slice);
        BlockHelper.assertSequence(iterator,Collections.<BlockEntry>emptyList());

        long endKey = table.getApproximatedOffsetOf(slice);
        Assert.assertTrue(endKey >= lastApproximatedOffset);
    }

    protected abstract Table createTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean checksum) throws IOException;

    @BeforeMethod
    public void setup() throws IOException {
        openTable();
        Preconditions.checkState(fileChannel.position() ==0);
    }

    @AfterMethod
    public void tearDown(){
        Closeables.close(fileChannel);
        Closeables.close(randomAccessFile);
        file.delete();
    }

    public void openTable() throws IOException {
        file = File.createTempFile("tmp", ".db");
        file.delete();
        randomAccessFile = new RandomAccessFile(file,"rw");
        fileChannel = randomAccessFile.getChannel();
    }
}
