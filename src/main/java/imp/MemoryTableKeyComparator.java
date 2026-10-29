package imp;

import utils.Slice;

import java.util.Comparator;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 21:39
 **/
public class MemoryTableKeyComparator implements Comparator<MemoryTableKey> {

    private final UserComparator comparator;

    public MemoryTableKeyComparator(UserComparator userComparator) {
        this.comparator = userComparator;
    }

    @Override
    public int compare(MemoryTableKey s1, MemoryTableKey s2) {
        int compare = comparator.compare(s1.getUserKey(), s2.getUserKey());
        return compare != 0 ? compare : Long.compare(s1.getSequenceId(), s2.getSequenceId());
    }

    public UserComparator getUserComparatorImp() {
        return comparator;
    }
}
