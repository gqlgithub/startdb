package imp;

import com.google.common.base.Preconditions;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-22 22:39
 **/
public class MmapLogWriter implements LogWriter {
    // todo 0923 use mmap log writer
    private final File file;

    private final long fileNumber;

    private final FileChannel fileChannel;

    private MappedByteBuffer mappedByteBuffer;

    private int blockOffset;

    private long filePosition;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private static final int MMAP_SIZE = 1024 * 1024;

    public MmapLogWriter(File file, long fileNumber) throws IOException {
        this.file = file;
        this.fileNumber = fileNumber;
        this.fileChannel = new RandomAccessFile(new File(file, FileNameGenerator.logFileName(fileNumber)), "RW").getChannel();
        this.mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MMAP_SIZE);
    }

    @Override
    public void appendLog(Slice record, boolean force) throws IOException {
        Preconditions.checkState(!closed.get(), "log writer is closed");

        SliceInput input = record.input();
        boolean start = true;
        do {
            int blockRemainSize = LogConstants.LOG_BLOCK_SIZE - blockOffset;
            if (blockRemainSize < LogConstants.HEADER_SIZE) {
                if (blockRemainSize > 0) {
                    ensureCapacity(blockRemainSize);
                    mappedByteBuffer.put(new byte[blockOffset]);
                }
                blockOffset = 0;
            }

            int realBytesInBlock = blockRemainSize - LogConstants.HEADER_SIZE;

            boolean end;
            int payload;
            if (input.available() > realBytesInBlock) {
                payload = realBytesInBlock;
                end = false;
            } else {
                payload = input.available();
                end = true;
            }

            Slice slice = input.readSlice(payload);

            LogType type;
            if (start & end) {
                type = LogType.FULL;
            } else if (start) {
                type = LogType.FIRST;
            } else if (end) {
                type = LogType.LAST;
            } else {
                type = LogType.MID;
            }

            writeChunk(slice, type);
            start = false;
        } while (input.isReadable());

        if (force) {
            mappedByteBuffer.force();
        }
    }

    public void writeChunk(Slice chuck, LogType type) throws IOException {
        Slice header = newLogHeader(chuck, type);
        ensureCapacity(header.length() + chuck.length());
        header.getBytes(0, mappedByteBuffer);
        chuck.getBytes(0, mappedByteBuffer);

        blockOffset += header.length() + chuck.length();
    }

    private void ensureCapacity(int capacity) throws IOException {
        if (mappedByteBuffer.remaining() < capacity) {
            filePosition += mappedByteBuffer.position();
            unmap();
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, filePosition, MMAP_SIZE);
        }
    }

    public Slice newLogHeader(Slice slice, LogType type) {
        SliceOutput header = Slices.alloc(LogConstants.HEADER_SIZE).output();

        header.writeInt(Logs
                .getChunkCheckSum(type.getPersistentId(), slice.getRawArray(), slice.getRawOffset(), slice.length()));
        header.writeByte((byte) slice.length());
        header.writeByte((byte) (slice.length() >>> 8));
        header.writeByte(type.getPersistentId());

        return header.slice();
    }


    public void unmap() {
        ByteBufferSupport.unmap(mappedByteBuffer);
    }

    public synchronized void delete() throws IOException {
        close();

        file.delete();
    }

    @Override
    public synchronized void close() throws IOException {
        closed.set(true);

        destroyMmapByteBuffer();

        try {
            if (fileChannel.isOpen()) {
                fileChannel.truncate(filePosition);
            }
        } finally {
            Closeables.close(fileChannel);
        }
    }

    public void destroyMmapByteBuffer() {
        if (mappedByteBuffer != null) {
            filePosition += mappedByteBuffer.position();
            unmap();
        }
        mappedByteBuffer = null;
    }


    public File getFile() {
        return file;
    }

    @Override
    public long getFileNumber() {
        return fileNumber;
    }

    public boolean isClosed() {
        return closed.get();
    }
}