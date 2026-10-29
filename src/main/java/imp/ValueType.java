package imp;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-03 18:10
 **/
public enum ValueType {

    VALUE(0),
    DELETION(1);

    private final byte persistentId;

    ValueType(int persistentId) {
        this.persistentId = (byte) persistentId;
    }

    public static ValueType getByPersistentId(int id){
        switch (id){
            case 0:
                return VALUE;
            case 1:
                return DELETION;
            default:
                throw new IllegalArgumentException("value type persistent id error");
        }
    }

    public byte getPersistentId() {
        return persistentId;
    }
}
