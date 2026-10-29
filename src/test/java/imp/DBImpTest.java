package imp;

import com.google.common.base.Preconditions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

/**
 * @Description:
 * @Author:
 * @Date: 2026-10-16 16:28
 **/
public class DBImpTest {
    private File databaseDir;

    @Test
    public void testBackGroundCompaction() throws IOException {
        Options options = new Options();
        DBImp db = new DBImp(databaseDir, options);

        Random random = new Random(300);
        for (int i = 0; i < 30; i++) {
            db.put(randomString(random, 640000 * 4).getBytes(), new byte[]{0x01}, new WriteOption().sync(true));
//            db.get(randomString(random, 64).getBytes());
            if (i != 0) {
                System.out.println("  写入行数 " + i);
            }
        }
    }

    public String randomString(Random random, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) ((int) ' ' + random.nextInt(95));
        }
        return new String(chars);
    }

    @BeforeMethod
    public void setup() throws IOException {
        databaseDir = FileUtils.createTempFile(new File("D:\\db"),"leveldb");
    }

    @AfterMethod
    public void tearDown() {
//        FileUtils.deleteRecursive(databaseDir);
    }
}
