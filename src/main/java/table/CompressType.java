package table;

import java.util.NoSuchElementException;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 11:44
 **/
public enum CompressType {
    NO_COMPRESS(0),

    SNAPPY(1);

    private final byte id;

    CompressType(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static CompressType getById(int id){
        for(CompressType type : CompressType.values()){
            if(type.id == id) return type;
        }

        throw new NoSuchElementException();
    }
}
