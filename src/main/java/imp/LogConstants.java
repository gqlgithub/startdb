package imp;

import utils.SizeOf;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 20:29
 **/
public class LogConstants {
    // header crc 4B length 2B type 1B
    public static final int HEADER_SIZE = SizeOf.INT + SizeOf.BYTE + SizeOf.SHORT;

    // block 32KB
    public static final int LOG_BLOCK_SIZE = 32768;
}
