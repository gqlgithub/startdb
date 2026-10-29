package imp;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-01 18:05
 **/
public class WALog {
    public static LogWriter createLogWriter(File file, long fileNumber) throws FileNotFoundException {
        return new FileChannelLogWriter(file,fileNumber);
    }
}
