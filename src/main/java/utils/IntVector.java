package utils;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-05 17:27
 **/
public class IntVector {

    private int[] values;
    private int size;

    public IntVector(int capacity) {
        values = new int[capacity];
    }

    public int[] getValues() {
        return values;
    }

    public void add(int position) {
        values = ensureSize(values, size + 1);
        values[size++] = position;
    }

    public void write(DynamicSliceOutput block) {
        for (int i = 0; i < size; i++) {
            block.writeInt(values[i]);
        }
    }

    public int[] ensureSize(int[] cur, int expectSize) {
        if (cur.length >= expectSize) {
            return cur;
        }

        int newSize = cur.length;
        while (newSize < expectSize) {
            newSize <<= 1;
        }

        int[] newValues = new int[newSize];

        //todo fix 1021 new values newsize cur 0 -----------> cur 0 new values 0
        System.arraycopy(cur, 0, newValues, 0, cur.length);
        return newValues;
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }
}
