package table;

import org.testng.Assert;
import org.testng.annotations.Test;
import utils.Slice;
import utils.SliceOutput;
import utils.Slices;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Description:
 * @Author:
 * @Date: 2026-10-08 15:34
 **/
public class BlockTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyBuffer() {
        new Block(Slices.EMPTY_SLICE, new BytewiseComparator());
    }

    @Test
    public void testBlockWithRestartAndNoSharing() {
        ArrayList<BlockEntry> list = new ArrayList<>();
        list.add(BlockHelper.createBlockEntry("hbase/db", "kv"));
        list.add(BlockHelper.createBlockEntry("level/db", "kv"));
        list.add(BlockHelper.createBlockEntry("rocks/db", "kv"));

        for (int i = 1; i < list.size(); i++) {
            testBlock(i, list);
        }
    }

    @Test
    public void testBlockWithRestartAndSharing() {
        ArrayList<BlockEntry> list = new ArrayList<>();
        list.add(BlockHelper.createBlockEntry("db/hbase/db", "kv"));
        list.add(BlockHelper.createBlockEntry("db/level/db", "kv"));
        list.add(BlockHelper.createBlockEntry("db/rocks/db", "kv"));
        list.add(BlockHelper.createBlockEntry("hbase/db", "kv"));
        list.add(BlockHelper.createBlockEntry("level/db", "kv"));
        list.add(BlockHelper.createBlockEntry("rocks/db", "kv"));

        for (int i = 1; i < list.size(); i++) {
            testBlock(i, list);
        }
    }


    public void testBlock(int restartCnt, List<BlockEntry> entries) {
        BlockBuilder blockBuilder = new BlockBuilder(512, restartCnt, new BytewiseComparator());
        for (BlockEntry entry : entries) {
            // fix
            blockBuilder.add(entry);
        }

        Assert.assertEquals(blockBuilder.computeCurrentSize(), BlockHelper.estimateBlockSize(restartCnt, entries));
        Slice blockSlice = blockBuilder.finish();
        Assert.assertEquals(blockBuilder.computeCurrentSize(), BlockHelper.estimateBlockSize(restartCnt, entries));

        Block block = new Block(blockSlice, new BytewiseComparator());
        // ------ fix
        Assert.assertEquals(block.size(), BlockHelper.estimateBlockSize(restartCnt, entries));

        BlockIterator iterator = block.iterator();
        // ------ fix
        BlockHelper.assertSequence(iterator, entries);

        iterator.seekToFirst();
        BlockHelper.assertSequence(iterator, entries);

        for (int i = 0; i < entries.size(); i++) {
            BlockEntry entry = entries.get(i);
            List<BlockEntry> subList = entries.subList(i, entries.size());
            iterator.seek(entry.getKey());
            BlockHelper.assertSequence(iterator, subList);

            iterator.seek(BlockHelper.before(entry));
            BlockHelper.assertSequence(iterator, subList);

            iterator.seek(BlockHelper.after(entry));
            BlockHelper.assertSequence(iterator, subList.subList(1, subList.size()));
        }
        Slice slice = Slices.wrapBuffer(new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
        iterator.seek(slice);
        BlockHelper.assertSequence(iterator, Collections.<BlockEntry>emptyList());
    }

//    @Test
//    public void test() {
//        BlockEntry blockEntry = BlockHelper.createBlockEntry("db/hbase/db", "kv");
//
//        Slice key1 = Slices.alloc(20);
//        SliceOutput output = key1.output();
//        output.writeBytes(blockEntry.getKey(), 0, 3);
//
//        Slice key2 = Slices.alloc(20);
//        SliceOutput output2 = key2.output();
//        output2.writeBytes(blockEntry.getKey().slice(0, 3));
//
//
//        for (int i = 0; i < key1.length(); i++) {
//            System.out.println(key1.getRawArray()[i]);
//        }
//        System.out.println("---------");
//        for (int i = 0; i < key2.length(); i++) {
//            System.out.println(key2.getRawArray()[i]);
//        }
//
//
//        System.out.println(key1.toString(StandardCharsets.UTF_8));
//        System.out.println(key2.toString(StandardCharsets.UTF_8));
//    }


}
