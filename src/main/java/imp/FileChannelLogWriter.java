package imp;

import utils.Closeables;
import utils.Slice;
import utils.SliceInput;
import utils.SliceOutput;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 16:14
 **/
public class FileChannelLogWriter implements LogWriter {
    private final long fileNumber;
    private final File file;
    private final FileChannel fileChannel;
    private int blockOffset;
    private final AtomicBoolean closed = new AtomicBoolean();

    public FileChannelLogWriter(File file, long fileNumber) throws FileNotFoundException {
        this.fileNumber = fileNumber;
        this.file = file;
        this.fileChannel = new FileOutputStream(file).getChannel();
    }

    @Override
    public void appendLog(Slice slice, boolean force) throws IOException {

        SliceInput sliceInput = slice.input();

        boolean start = true;

        do {
            int remainBlockBufferSize = LogConstants.LOG_BLOCK_SIZE - blockOffset;
            if (remainBlockBufferSize < LogConstants.HEADER_SIZE) {
                if (remainBlockBufferSize > 0) {
                    // padding zero
                    fileChannel.write(ByteBuffer.allocate(remainBlockBufferSize));
                }
                // new block
                blockOffset = 0;
                remainBlockBufferSize = LogConstants.LOG_BLOCK_SIZE;
            }

            int availableBlockBufferSize = remainBlockBufferSize - LogConstants.HEADER_SIZE;

            boolean end;
            int payload;
            if (sliceInput.available() > availableBlockBufferSize) {
                payload = availableBlockBufferSize;
                end = false;
            } else {
                payload = sliceInput.available();
                end = true;
            }

            LogType logType;
            if (start & end) {
                logType = LogType.FULL;
            } else if (start) {
                logType = LogType.FIRST;
            } else if (end) {
                logType = LogType.LAST;
            } else {
                logType = LogType.MID;
            }

            writeChunk(logType, sliceInput.readSlice(payload));
            start = false;
        } while (sliceInput.isReadable());

        if (force) {
            fileChannel.force(false);
        }
    }

    private void writeChunk(LogType logType, Slice slice) throws IOException {
        int crc = Logs.getChunkCheckSum(logType.getPersistentId(), slice.getRawArray(), slice.getRawOffset(), slice.length());

        Slice header = newLogHeader(crc, logType, slice);
        header.getBytes(0, fileChannel, header.length());
        slice.getBytes(0, fileChannel, slice.length());

        blockOffset += LogConstants.HEADER_SIZE + slice.length();
    }

    private Slice newLogHeader(int crc, LogType logType, Slice slice) {
        SliceOutput header = Slice.alloc(LogConstants.HEADER_SIZE).output();
        int length = slice.length();
        header.writeInt(crc);
        header.writeByte((byte) (length & 0xff));
        header.writeByte((byte) (length >>> 8));
        header.writeByte(logType.getPersistentId());
        return header.slice();
    }

    @Override
    public synchronized void close() {
        closed.set(true);

        try {
            fileChannel.force(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Closeables.close(fileChannel);
    }

    public synchronized void delete(){
        closed.set(true);

        try {
            fileChannel.force(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Closeables.close(fileChannel);
        file.delete();
    }

    public long getFileNumber() {
        return fileNumber;
    }

    public File getFile() {
        return file;
    }

    public boolean isClosed(){
        return closed.get();
    }
}