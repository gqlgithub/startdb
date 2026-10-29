package imp;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-01 17:56
 **/
public class WriteOption {
    private boolean sync;

    public boolean fsc() {
        return sync;
    }

    public WriteOption sync(boolean fsc) {
        this.sync = fsc;
        return this;
    }
}
