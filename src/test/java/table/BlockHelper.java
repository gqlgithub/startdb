package table;

import imp.SeekingIterable;
import imp.SeekingIterator;
import org.testng.Assert;
import utils.SizeOf;
import utils.Slice;
import utils.Slices;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static javax.swing.text.StyleConstants.Size;

/**
 * @Description:
 * @Author:
 * @Date: 2026-10-08 16:28
 **/
public class BlockHelper {

    public static int estimateBlockSize(int restartCnt, List<BlockEntry> entries) {
//        int cnt = (int) Math.ceil(entries.size() * 1.0 / restartCnt);
        int cnt = (entries.size() + restartCnt - 1) / restartCnt;
        return estimateEntriesSize(restartCnt, entries)
                + cnt * SizeOf.INT
                + SizeOf.INT;
    }

    private static int estimateEntriesSize(int restartCnt, List<BlockEntry> entries) {
        int size = 0;
        int cnt = 0;
        Slice preEntryKey = null;

        for (BlockEntry entry : entries) {
            int noSharingBytes;
            if (cnt < restartCnt) {
                noSharingBytes = entry.getKey().length()
                        - BlockBuilder.computeShardingPrefixSize(preEntryKey, entry.getKey());
            } else {
                noSharingBytes = entry.getKey().length();
                cnt = 0;
            }

            size += noSharingBytes + entry.getValue().length() + SizeOf.BYTE * 3;
            cnt++;
            preEntryKey = entry.getKey();
        }

        return size;
    }

    public static Slice before(Map.Entry<Slice,Slice> entry){
        Slice key = entry.getKey();
        Slice newKey = key.copySlice();
        newKey.setByte(newKey.length() - 1,key.getByte(newKey.length()- 1) - 1);
        return newKey;
    }

    public static Slice after(Map.Entry<Slice,Slice> entry){
        Slice key = entry.getKey();
        Slice newKey = key.copySlice();
        newKey.setByte(newKey.length() - 1,key.getByte(newKey.length()- 1) + 1);
        return newKey;
    }

    public static <K, V> void assertSequence(SeekingIterator<K, V> iterator, Iterable<? extends Map.Entry<K, V>> entries) {
        Assert.assertNotNull(iterator, "iterator must be not null");

        for (Map.Entry<K, V> entry : entries) {
            Assert.assertTrue(iterator.hasNext());
            assertEntryEqual(iterator.peek(), entry);
            assertEntryEqual(iterator.next(), entry);
        }

        Assert.assertFalse(iterator.hasNext());
    }

    public static <K, V> void assertEntryEqual(Map.Entry<K, V> actual, Map.Entry<K, V> expect) {
        if (actual.getKey() instanceof Slice) {
            assertSliceEqual((Slice)actual.getKey(),(Slice)expect.getKey());
            assertSliceEqual((Slice)actual.getValue(),(Slice)expect.getValue());
        }
        Assert.assertEquals(actual, expect);
    }

    public static void assertSliceEqual(Slice actual, Slice expect) {
        Assert.assertEquals(actual.toString(StandardCharsets.UTF_8), expect.toString(StandardCharsets.UTF_8));
    }


    public static BlockEntry createBlockEntry(String key, String value) {
        return new BlockEntry(
                Slices.copiedBuffer(key, StandardCharsets.UTF_8), Slices.copiedBuffer(value, StandardCharsets.UTF_8));
    }
}