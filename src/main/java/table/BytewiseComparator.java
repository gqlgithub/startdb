package table;

import imp.UserComparator;
import utils.Slice;
import utils.Slices;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 10:58
 **/
public class BytewiseComparator implements UserComparator {

    @Override
    public int compare(Slice s1, Slice s2) {
        return s1.compareTo(s2);
    }

    public Slice findShortestSeparator(Slice lastKey, Slice key) {

        int shardingPrefixSize = BlockBuilder.computeShardingPrefixSize(lastKey, key);

        int lastKeyByte = lastKey.getUnsignedByte(shardingPrefixSize);
        if (lastKeyByte < Math.min(lastKey.length(), key.length())) {
            if (lastKeyByte < 0xff && lastKeyByte + 1 < key.getByte(shardingPrefixSize)) {
                Slice shortKey = lastKey.copySlice(0, shardingPrefixSize + 1);
                shortKey.setByte(shardingPrefixSize, lastKeyByte + 1);

                assert (shortKey.compareTo(key) < 0);
                return shortKey;
            }
        }

        return lastKey;
    }

    public Slice findShortestSuccessor(Slice key) {
        for (int i = 0; i < key.length(); i++) {
            int b = key.getUnsignedByte(i);
            if (b != 0xff) {
                Slice shortKey = key.copySlice(0, i + 1);
                shortKey.setByte(i, b + 1);
                return shortKey;
            }
        }
        // 0xffs don't  short
        return key;
    }

}
