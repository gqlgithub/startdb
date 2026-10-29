package utils;

import com.google.common.collect.ImmutableList;

import java.io.File;

/**
 * @Description:
 * @Author:
 * @Date: 2026-10-16 16:36
 **/
public class FileUtils {

    public final static int TMP_DIR_ATTEMPTS = 10000;

    public static File createTempFile(String prefix) {
        return createTempFile(new File(System.getProperty("java.io.tmpdir")), prefix);
    }

    public static File createTempFile(File parentDir, String prefix) {
        String baseName = "";

        if (prefix != null) {
            baseName += prefix + "-";
        }

        baseName += System.currentTimeMillis() + "-";
        for (int i = 0; i < TMP_DIR_ATTEMPTS; i++) {
            File tmpDir = new File(parentDir, baseName + i);
            if (tmpDir.mkdir()) {
                return tmpDir;
            }
        }

        throw new IllegalStateException("create tmp dir attempt > TMP_DIR_ATTEMPTS");
    }

    public static boolean deleteRecursive(File file) {
        if (file.isFile()) {
            return file.delete();
        }
        return deleteDirContents(file);
    }

    private static boolean deleteDirContents(File dir) {
        boolean result = true;

        for (File file : listFile(dir)) {
            result &= deleteRecursive(file);
        }
        result &= dir.delete();

        return result;
    }

    public static ImmutableList<File> listFile(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(files);
    }
}
