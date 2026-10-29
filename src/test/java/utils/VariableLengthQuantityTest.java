package utils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-03 17:00
 **/
public class VariableLengthQuantityTest {
    @Test
    public static void testWriteVariableLengthInt() {
        testWriteVariableLengthInt(0x00);
        testWriteVariableLengthInt(0x0f);
        testWriteVariableLengthInt(0xff);
        testWriteVariableLengthInt(0x0fff);
        testWriteVariableLengthInt(0xffff);
        testWriteVariableLengthInt(0x0fffff);
        testWriteVariableLengthInt(0xffffff);
        testWriteVariableLengthInt(0x0fffffff);
        testWriteVariableLengthInt(0xffffffff);
    }

    public static void testWriteVariableLengthInt(int value) {
        Slice slice = Slice.alloc(5);
        SliceOutput output = slice.output();
        VariableLengthQuantity.writeVariableLengthInt(output, value);

        int length = output.size();
        int size = VariableLengthQuantity.variableLengthByteSize(value);
        Assert.assertEquals(size, length);

        SliceInput input = output.slice().input();
        int reallyValue = VariableLengthQuantity.readVariableLengthInt(input);
        Assert.assertEquals(reallyValue, value);
    }

}
