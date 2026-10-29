package imp;

import utils.Slice;

import java.io.IOException;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 16:04
 **/
public interface LogWriter {

    void appendLog(Slice slice, boolean force) throws IOException;

    long getFileNumber();

    void close() throws IOException;
}
