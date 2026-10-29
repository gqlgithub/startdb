package imp;

import utils.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-21 7:36
 **/
public class LogReader {

    private final FileChannel fileChannel;
    private final long initialOffset;
    private final boolean checkSum;
    private final LogMintor logMintor;

    private Slice currentChuck;
    private SliceInput currentBlock;
    private final SliceOutput blockScratch = Slices.alloc(LogConstants.LOG_BLOCK_SIZE).output();
    private final DynamicSliceOutput recordScratch = new DynamicSliceOutput(Slices.EMPTY_SLICE.length());


    private long lastRecordOffset;
    private long endBlockBufferOffset;

    private boolean eof = false;

    public LogReader(FileChannel fileChannel, long initialOffset, boolean checkSum, LogMintor logMintor) {
        this.fileChannel = fileChannel;
        this.initialOffset = initialOffset;
        this.checkSum = checkSum;
        this.logMintor = logMintor;
    }

    public void skipToInitialBlockOffset() {
        int blockOffset = (int) (initialOffset % LogConstants.LOG_BLOCK_SIZE);

        endBlockBufferOffset = initialOffset - blockOffset;
        if (LogConstants.LOG_BLOCK_SIZE - blockOffset < 6) {
            endBlockBufferOffset = endBlockBufferOffset + LogConstants.LOG_BLOCK_SIZE;
        }


    }

    public Slice readRecord() throws IOException {

        long prospectiveRecordOffset = 0;

        while (true) {
            // todo diff
            long realLastRecordOffset =
                    endBlockBufferOffset - currentBlock.available() - LogConstants.HEADER_SIZE;
            LogType logType = readNextChuck();

            switch (logType) {
                case FULL: {
                    recordScratch.reset();
                    prospectiveRecordOffset = realLastRecordOffset;
                    lastRecordOffset = prospectiveRecordOffset;
                    return currentChuck.copySlice();
                }
                case FIRST: {
                    recordScratch.reset();
                    prospectiveRecordOffset = realLastRecordOffset;
                    recordScratch.writeBytes(currentChuck);
                    break;
                }
                case MID: {
                    recordScratch.writeBytes(currentChuck);
                    break;
                }
                case LAST: {
                    lastRecordOffset = prospectiveRecordOffset;
                    recordScratch.writeBytes(currentChuck);
                    return recordScratch.slice().copySlice();
                }
                case BAD_CHUCK: {
                    break;
                }
                case EOF: {
                    return null;
                }
                default: {

                }
            }
        }

    }

    public LogType readNextChuck() throws IOException {
        if (currentBlock.available() < LogConstants.HEADER_SIZE) {
            // todo fix 1029 || currentBlock.available() < 7
            if (!readNextBlock() || currentBlock.available() < 7) {
                if (eof) {
                    return LogType.EOF;
                }
            }
        }

        int crc = currentBlock.readInt();
        int length = currentBlock.readByte() << 8 | currentBlock.readByte();
        LogType type = LogType.findByPersistentId(currentBlock.readByte());

        currentChuck = currentBlock.readSlice(length);

        if (checkSum) {
//            todo chuck check sum
            checkCrcSum(crc, currentChuck);
        }

        return type;
    }

    public boolean readNextBlock() {
        if (eof) {
            return false;
        }

        blockScratch.reset();

        while (blockScratch.writeableBytes() > 0) {
            try {
                int readBytes = blockScratch.writeBytes(fileChannel, blockScratch.writeableBytes());
                if (readBytes < 0) {
                    eof = true;
                    break;
                }
                endBlockBufferOffset += readBytes;
            } catch (Exception e) {
                eof = true;
                currentBlock = Slices.EMPTY_SLICE.input();
                return false;
            }
        }

        currentBlock = blockScratch.slice().input();
        return currentBlock.isReadable();
    }


    private boolean checkCrcSum(int crc, Slice currentChuck) {
        return true;
    }
}
