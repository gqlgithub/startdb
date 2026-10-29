package api;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-03 9:59
 **/
public interface WriteBatch {

    WriteBatch put(byte[] key, byte[] value);

    WriteBatch delete(byte[] key);

}
