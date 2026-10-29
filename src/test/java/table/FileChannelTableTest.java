package table;

import utils.Slice;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Comparator;

/**
 * @Description:
 * @Author:
 * @Date: 2026-10-12 16:04
 **/
public class FileChannelTableTest extends TableTest{
    @Override
    protected Table createTable(String name, FileChannel fileChannel, Comparator<Slice> comparator, boolean checksum) throws IOException {
        return new FileChannelTable(name, fileChannel, comparator, checksum);
    }
}
