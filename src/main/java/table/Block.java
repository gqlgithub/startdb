package table;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import imp.SeekingIterable;
import imp.UserComparatorImp;
import utils.SizeOf;
import utils.Slice;
import utils.Slices;

import java.util.Comparator;
import java.util.Map.Entry;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-11 14:31
 **/
public class Block implements SeekingIterable<Slice, Slice>{
    private final Slice block;
    private final Slice data;
    private final Slice restartPositions;
    private final Comparator<Slice> comparator;

    Block(Slice block, Comparator<Slice> comparator) {
        checkState(block != null,"block is null");
        Preconditions.checkArgument(block.length() >= SizeOf.INT, "block size at lease %s",SizeOf.INT);
        checkState(comparator != null, "comparator is null");

        this.block = block.slice();
        this.comparator = comparator;

        for (int i = 0; i < block.length(); i++) {
            System.out.println(block.getByte(i));
        }

        int restartCnt = block.getInt(block.length() - SizeOf.INT);

        System.out.println("restartCnt " +restartCnt);

        if(restartCnt > 0){
            int restartOffset = block.length() - (1 + restartCnt) * SizeOf.INT;
            checkState(restartOffset < block.length() - SizeOf.INT, "block restart offset greater than restart count start");
            restartPositions = block.slice(restartOffset, restartCnt * SizeOf.INT);
            data = block.slice(0, restartOffset);
        }else {
            data = Slices.EMPTY_SLICE;
            restartPositions = Slices.EMPTY_SLICE;
        }
    }

    public BlockIterator iterator() {
        System.out.println("restartPositions length " + restartPositions.length());

        return new BlockIterator(data, restartPositions, comparator);
    }

    public Slice getRawBlock(){
        return block;
    }

    public int size(){
       return block.length();
    }
}
