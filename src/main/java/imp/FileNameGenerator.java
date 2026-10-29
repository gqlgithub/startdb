package imp;

import com.google.common.collect.ImmutableList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 16:07
 **/
public class FileNameGenerator {

    public static String logFileName(long logNumber) {
        return makeFileName(logNumber, "log");
    }

    public static String tableFileName(long fileNumber) {
        return makeFileName(fileNumber, "sst");
    }

    public static String descriptorFile(long manifestNumber) {
        return String.format("MANIFEST-%06d", manifestNumber);
    }

    public static String makeFileName(long number, String suffix) {
        return String.format("%06d.%s", number, suffix);
    }

    public static void setCurrentManifest(File databaseDir, String manifestNumber) throws IOException {
        File currentTemp = new File(databaseDir, "CURRENT_TEMP");
        writeTo(currentTemp, manifestNumber);
        File current = new File(databaseDir, "CURRENT");

        boolean rename = currentTemp.renameTo(current);
        if (!rename) {
            throw new IOException("rename false");
        }
    }

    public static void writeTo(File file, String content) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file));
        dataOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    public static List<File> listFiles(File databaseDir) {
//        ArrayList<File> list = new ArrayList<>();
        File[] files = databaseDir.listFiles();
        if (files == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(files);
    }

    public static FileInfo parseFileName(File fileName) {
        String name = fileName.getName();
        if (name.endsWith(".log")) {
            long fileNumber = Long.parseLong(name.substring(0, name.length() - ".log".length()));
            return new FileInfo(FileType.LOG, fileNumber);
        }
        return null;
    }

    public static class FileInfo {
        private FileType type;
        private Long fileNumber;

        public FileInfo(FileType type, Long fileNumber) {
            this.type = type;
            this.fileNumber = fileNumber;
        }

        public FileType getType() {
            return type;
        }

        public Long getFileNumber() {
            return fileNumber;
        }
    }

    public enum FileType {
        LOG,
        SST
    }
}
