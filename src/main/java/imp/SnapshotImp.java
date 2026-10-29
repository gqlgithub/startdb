package imp;

import api.Snapshot;

import java.io.IOException;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-14 22:53
 **/
public class SnapshotImp implements Snapshot {
// todo
    private final Version version;
    private final long lastSequenceId;

    public SnapshotImp(Version version, long lastSequenceId) {
        this.version = version;
        this.lastSequenceId = lastSequenceId;
    }

    public Version getVersion() {
        return version;
    }

    public long getLastSequenceId() {
        return lastSequenceId;
    }

    @Override
    public void close() throws IOException {

    }
}
