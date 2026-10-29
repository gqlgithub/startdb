package table;

import static com.google.common.base.Preconditions.checkState;

import imp.SeekingIterator;
import utils.*;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-11 14:31
 **/
public class BlockIterator implements SeekingIterator<Slice, Slice> {

    private final SliceInput data;
    private final Slice restartPositions;
    private final int restartCnt;
    private final Comparator<Slice> comparator;

    private BlockEntry next;

    public BlockIterator(Slice blockData, Slice restartPositions, Comparator<Slice> comparator) {
        checkState(blockData != null, "blockData is null");
        checkState(restartPositions != null, "restartPositions is null");
        this.data = blockData.input();
        this.restartPositions = restartPositions;
        this.restartCnt = restartPositions.length() / SizeOf.INT;
        this.comparator = comparator;

        // todo fix
        seekToFirst();
    }

    @Override
    public void seekToFirst() {
        // todo fix seek first but is  empty-block size == 0
        if (restartPositions.length() > 0){
            System.out.println("---------");
            seekRestartPosition(0);
        }
//        System.out.println("BlockEntry " + next.getKey());
    }

    @Override
    public void seek(Slice key) {
        if (restartPositions.length() == 0) return;

        int left = 0;
        int right = restartCnt - 1;

        int minMostRight = left;
        while (left <= right) {
            int mid = left + ((right - left) >> 1);
            seekRestartPosition(mid);
            if (comparator.compare(next.getKey(), key) > 0) {
                left = mid + 1;
            } else {
                minMostRight = left;
                right = mid - 1;
            }
        }

        for (seekRestartPosition(minMostRight); next != null; next()) {
            if (comparator.compare(next.getKey(), key) >= 0) {
                break;
            }
        }
    }

    public void seekRestartPosition(int position) {

        int index = position * SizeOf.INT;
        data.setPos(restartPositions.getInt(index));

        System.out.println(next == null);

        next = null;
        next = readEntry(null);

        System.out.println("seek 0 =========" + next);
        for (int i = 0; i < next.getKey().length(); i++) {
            System.out.println("next ================="+next.getKey().getByte(i));
        }
    }

    @Override
    public boolean hasNext() {
        return next != null || data.isReadable();
    }

    @Override
    public Entry<Slice, Slice> next() {
        //  decode <key value>
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        BlockEntry entry = next;
        if (!data.isReadable()) {
            next = null;
        } else {
            next = readEntry(next);
        }

        return entry;
    }

    @Override
    public void remove() {

    }

    @Override
    public Entry<Slice, Slice> peek() {
//        for (int i = 0; i < next.getKey().length(); i++) {
//            System.out.println("next ================="+next.getKey().getByte(i));
//        }
//        System.out.println();
        if (!hasNext()) {
            return null;
        }

        return next;
    }

    private BlockEntry readEntry(BlockEntry preEntry) {

//        for (int i = 0; data.available()>0; i++) {
//            System.out.println("next ================="+data.readByte());
//        }
//
//        data.setPos(0);

        // <sharing key length>  <no-sharing key length> <value length>  <no-sharing key> <value>
        int sharingKeyLength = VariableLengthQuantity.readVariableLengthInt(data);
        int noSharingKeyLength = VariableLengthQuantity.readVariableLengthInt(data);
        int valueLength = VariableLengthQuantity.readVariableLengthInt(data);

        Slice key;
        if (sharingKeyLength == 0) {
            key = data.readSlice(noSharingKeyLength);
        } else {
            key = Slices.alloc(sharingKeyLength + noSharingKeyLength);
            SliceOutput output = key.output();
            checkState(preEntry != null, "preEntry is null");
//            output.writeBytes(preEntry.getKey(),0, sharingKeyLength);
            output.writeBytes(preEntry.getKey().slice(0, sharingKeyLength));
            output.writeBytes(data.readSlice(noSharingKeyLength));
        }

//        System.out.println("key "+key.length());
//        System.out.println("key "+key.toString(StandardCharsets.UTF_8));

        return new BlockEntry(key, data.readSlice(valueLength));
    }
}