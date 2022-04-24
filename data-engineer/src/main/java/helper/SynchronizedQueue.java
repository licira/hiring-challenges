package helper;

import java.util.*;

public class SynchronizedQueue<E> extends LinkedList<E> {

    public synchronized boolean add(final E e) {
        return super.add(e);
    }

    public synchronized boolean addAll(final Collection<? extends E> elements) {
        return super.addAll(elements);
    }

    public List<E> pollAll() {
        final List<E> polled = new ArrayList<>(this.size());
        synchronized (this) {
            while (!isEmpty()) {
                polled.add(poll());
            }
        }
        return polled;
    }

    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }
}
