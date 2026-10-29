package table;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import imp.UserComparator;
import utils.*;

import java.nio.charset.StandardCharsets;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 17:22
 **/
public class BlockBuilder {

    private final DynamicSliceOutput block;

    private final int restartPrefixCount;
    private final IntVector restartPositions;

    private int entrySize;
    private int restartEntrySize;

    private final UserComparator comparator;
    private Slice lastKey;

    private boolean finished;

    public BlockBuilder(int blockSize, int restartCnt, UserComparator userComparator) {
        Preconditions.checkArgument(restartCnt > 0,"restart count must be positive");

        block = new DynamicSliceOutput(blockSize);
        restartPrefixCount = restartCnt;
        comparator = userComparator;

        restartPositions = new IntVector(32);
        restartPositions.add(0);

        entrySize = 0;
        restartEntrySize = 0;
    }


    public void add(BlockEntry entry) {
        add(entry.getKey(), entry.getValue());
    }

    public void add(Slice key, Slice value) {

        checkState(!finished, "current block is finished");

//        System.out.println(key.toString(StandardCharsets.UTF_8));

        checkState(lastKey == null || comparator.compare(lastKey, key) < 0, "key must be great than lastKey");


        int shardingPrefixSize = 0;
        if (restartEntrySize < restartPrefixCount) {
            // current group
            shardingPrefixSize = computeShardingPrefixSize(lastKey, key);
        } else {
            // start new group
            restartPositions.add(block.size());
            restartEntrySize = 0;
        }

        // [sharding key len] [no-sharding key len] [value len]
        VariableLengthQuantity.writeVariableLengthInt(block, shardingPrefixSize);
        VariableLengthQuantity.writeVariableLengthInt(block, key.length() - shardingPrefixSize);
        VariableLengthQuantity.writeVariableLengthInt(block, value.length());
        // no-sharing key
        block.writeBytes(key, shardingPrefixSize, key.length() - shardingPrefixSize);

        // value
        block.writeBytes(value, 0, value.length());

        lastKey = key;
        entrySize++;
        restartEntrySize++;
    }

    public static int computeShardingPrefixSize(Slice lastKey, Slice key) {
        int size = 0;
        // todo
        if (lastKey != null) {
            for (int i = 0; i < lastKey.length() && i < key.length(); i++) {
                if (key.getByte(i) == lastKey.getByte(i)) {
                    size++;
                } else {
                    break;
                }
            }
        }
        return size;
    }

    public int computeCurrentSize() {
        if (finished) {
            return block.size();
        }

        if (entrySize == 0) {
            return SizeOf.INT;
        }

        return block.size() + SizeOf.INT * restartPositions.size() + SizeOf.INT;
    }

    public Slice finish() {
        if (!finished) {
            finished = true;

            System.out.println("entrySize " + entrySize);
            System.out.println("restartPositionsSize  " + restartPositions.size());
            if (entrySize > 0) {
                restartPositions.write(block);
                block.writeInt(restartPositions.size());
            } else {
                block.writeInt(0);
            }
        }

        return block.slice();
    }

    public void reset() {
        block.reset();
        restartPositions.clear();
        restartPositions.add(0);

        entrySize = 0;
        restartEntrySize = 0;

        lastKey = null;
        finished = false;
    }

    public boolean isEmpty() {
        return entrySize == 0;
    }

}
