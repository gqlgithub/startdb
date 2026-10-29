package utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 22:42
 **/
public class Closeables {

    public static void close(Closeable closeable){
        if(closeable == null) return;

        try {
            closeable.close();
        } catch (IOException ignore) {

        }
    }
}
