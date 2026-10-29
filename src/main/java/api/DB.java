package api;

import imp.WriteOption;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-01 17:45
 **/
public interface DB {
    void put(byte[] key, byte[] value);
    void put(byte[] key, byte[] value, WriteOption option);
}
