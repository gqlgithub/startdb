package imp;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-02 21:19
 **/
public enum LogType {
    FULL(0),
    FIRST(1),
    MID(2),
    LAST(3),

    EOF(4),
    UNKNOWN(5),
    BAD_CHUCK(6);

    private final Integer persistentId;

    LogType() {
        persistentId = null;
    }

    LogType(int persistentId) {
        this.persistentId = persistentId;
    }

    public static LogType findByPersistentId(int persistentId) {
        for (LogType logType : LogType.values()) {
            if (logType.persistentId == persistentId) return logType;
        }
        return UNKNOWN;
    }

    public int getPersistentId() {
        checkArgument(persistentId != null, "log type persistent id is null");
        return persistentId;
    }
}
