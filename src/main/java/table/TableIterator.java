package table;

import imp.SeekingIterator;
import utils.AbstractSeekingIterator;
import utils.Slice;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-11 14:31
 **/
public class TableIterator
        extends AbstractSeekingIterator<Slice, Slice> {

    private final Table table;

    private final SeekingIterator<Slice, Slice> indexBlockIter;

    private SeekingIterator<Slice, Slice> blockIter;

    public TableIterator(Table table, SeekingIterator<Slice, Slice> indexBlockIter) {
        this.table = table;
        this.indexBlockIter = indexBlockIter;

        seekToFirst();
    }

    @Override
    public void seekFirstInternal() {
        indexBlockIter.seekToFirst();
        blockIter = null;
    }

    @Override
    public void seekEntryInternal(Slice key) {
        indexBlockIter.seek(key);
        if (indexBlockIter.hasNext()) {
            Slice blockHandle = indexBlockIter.next().getValue();
            blockIter = table.openBlock(blockHandle).iterator();
            blockIter.seek(key);
        }
    }

    @Override
    public boolean hasNext() {
//        System.out.println(indexBlockIter.hasNext());
        if (nextElement == null) {
            nextElement = getNextElement();
            if (nextElement == null) {
                return (blockIter != null && blockIter.hasNext()) || indexBlockIter.hasNext();
            }
        }

        return true;
    }

    public Entry<Slice, Slice> getNextElement() {
//        System.out.println("nextEntry =============== ");
//        Entry<Slice, Slice> result = next;
        Entry<Slice, Slice> result = null;
        if (blockIter != null && blockIter.hasNext()) {
            Entry<Slice, Slice> nextEntry = blockIter.next();
//            System.out.println("nextEntry -------------- " + nextEntry.getKey().toString(StandardCharsets.UTF_8));
            result = nextEntry;
        } else if (indexBlockIter.hasNext()) {
            Entry<Slice, Slice> blockHandle = indexBlockIter.next();
            Block block = table.openBlock(blockHandle.getValue());
            blockIter = block.iterator();
            result = blockIter.next();
//            System.out.println("next -------------- " + next.getKey().toString(StandardCharsets.UTF_8));
        }

//        System.out.println("result ************** " + (result == null ? "result null " : result.getKey().toString(StandardCharsets.UTF_8))
//                + " " + (next == null ? " next null " : next.getKey().toString(StandardCharsets.UTF_8)));
//        if (result == next) next = null;
        return result;
    }
}
