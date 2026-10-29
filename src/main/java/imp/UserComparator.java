package imp;

import utils.Slice;

import java.util.Comparator;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 16:15
 **/
public interface UserComparator extends Comparator<Slice> {

    Slice findShortestSeparator(Slice s1 , Slice s2);
    Slice findShortestSuccessor(Slice s);
}
