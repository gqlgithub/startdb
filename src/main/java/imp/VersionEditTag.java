package imp;

import com.google.common.collect.ArrayListMultimap;
import utils.SliceInput;
import utils.SliceOutput;
import utils.Slices;
import utils.VariableLengthQuantity;

import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-09 14:41
 **/
public enum VersionEditTag {
    NEW_FILE(7) {
        @Override
        void writeEdit(SliceOutput output, VersionEdit edit) {

            for (Map.Entry<Integer, FileMeta> entry : edit.getNewFiles().entries()) {
                // <level> <fileNumber> <fileSize> <minKey> <maxKey>
                VariableLengthQuantity.writeVariableLengthInt(output, entry.getKey());
                VariableLengthQuantity.writeVariableLengthLong(output, entry.getValue().getFileNumber());
                VariableLengthQuantity.writeVariableLengthLong(output, entry.getValue().getFileLength());
                Slices.writeVariableLengthAndValue(output, entry.getValue().getMinKey().encode());
                Slices.writeVariableLengthAndValue(output, entry.getValue().getMaxKey().encode());
            }

        }

        @Override
        void readEdit(SliceInput input, VersionEdit edit) {

        }
    },

    UNK(8) {
        @Override
        void writeEdit(SliceOutput output, VersionEdit edit) {

        }

        @Override
        void readEdit(SliceInput input, VersionEdit edit) {

        }
    };

    private final int id;

    VersionEditTag(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static VersionEditTag findById(int id) {
        for (VersionEditTag tag : VersionEditTag.values()) {
            if (id == tag.getId()) {
                return tag;
            }
        }
        return UNK;
    }

    abstract void writeEdit(SliceOutput output, VersionEdit edit);

    abstract void readEdit(SliceInput input, VersionEdit edit);
}