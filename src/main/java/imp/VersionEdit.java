package imp;

import com.google.common.collect.ArrayListMultimap;
import utils.DynamicSliceOutput;
import utils.Slice;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-04 15:21
 **/
public class VersionEdit {
    private Long nextFileNumber;
    private Long lastSequence;
    private Long preLogNumber;
    private Long logNumber;

    private final ArrayListMultimap<Integer, FileMeta> newFiles = ArrayListMultimap.create();
    private final ArrayListMultimap<Integer, Long> deleteFiles = ArrayListMultimap.create();

    private final Map<Integer, MemoryTableKey> compactionPoints = new HashMap<>();

    public VersionEdit(){

    }
    public VersionEdit(Slice record) {
        VersionEdit edit = new VersionEdit();
        byte typeId = record.getByte(0);
        VersionEditTag editTag = VersionEditTag.findById(typeId);
        editTag.readEdit(record.input(),edit);
    }

    public Long getNextFileNumber() {
        return nextFileNumber;
    }

    public void setNextFileNumber(long nextFileNumber) {
        this.nextFileNumber = nextFileNumber;
    }

    public Long getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(long lastSequence) {
        this.lastSequence = lastSequence;
    }

    public Long getPreLogNumber() {
        return preLogNumber;
    }

    public void setPreLogNumber(long preLogNumber) {
        this.preLogNumber = preLogNumber;
    }

    public Long getLogNumber() {
        return logNumber;
    }

    public void setLogNumber(long logNumber) {
        this.logNumber = logNumber;
    }

    public void addFile(int level, FileMeta fileMeta) {
        newFiles.put(level, fileMeta);
    }

    public void deleteFiles(int level, FileMeta fileMeta) {
        deleteFiles.put(level, fileMeta.getFileNumber());
    }

    public ArrayListMultimap<Integer, FileMeta> getNewFiles() {
        return newFiles;
    }

    public ArrayListMultimap<Integer, Long> getDeleteFiles() {
        return deleteFiles;
    }

    public void addCompactionPoint(int level, MemoryTableKey key){
        compactionPoints.put(level, key);
    }

    // todo
    public Slice encode() {
        DynamicSliceOutput output = new DynamicSliceOutput(0);
        for (VersionEditTag editTag : VersionEditTag.values()) {
            editTag.writeEdit(output, this);
        }
        return output.slice();
    }
}
