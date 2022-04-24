package helper;

public class Count {

    private long val;

    public synchronized void add(final long inc) {
        this.val += inc;
    }

    public long get() {
        return val;
    }
}
