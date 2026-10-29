package imp;

import utils.Slice;
import utils.Slices;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 16:17
 **/
public class UserComparatorImp implements UserComparator{

    @Override
    public int compare(Slice s1, Slice s2) {
        return 0;
    }

    @Override
    public Slice findShortestSeparator(Slice s1, Slice s2) {
        return Slices.EMPTY_SLICE;
    }

    @Override
    public Slice findShortestSuccessor(Slice s) {
        return Slices.EMPTY_SLICE;
    }
}
