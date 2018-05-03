package cn.gg.fastqueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 *
 * @author gumandy
 * @date 2018年4月21日 下午1:43:52
 * @version 1.0
 *
 */
public class ConcurrentBlockingQueue<E> implements BlockingQueue<E> {
    // 队列深度
    private final int capacity;
    // 存储空间大小，大于或等于capacity的2^n
    private final int bufferSize;
    // bufferSize-1
    private final int indexMask;
    // 存储数组
    private final E[] entries;

    // 生产者游标
    private final AtomicLong headBeforeWirte = new AtomicLong(0);
    private final AtomicLong headAfterWirte = new AtomicLong(0);
    // 消费者游标
    private final AtomicLong tail = new AtomicLong(0);

    @SuppressWarnings("unchecked")
    public ConcurrentBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.bufferSize = ceilingNextPowerOfTwo(capacity);
        this.indexMask = bufferSize - 1;
        if (this.capacity < 1 || this.bufferSize < 1) {
            throw new IllegalArgumentException("capacity must more than 0");
        }
        this.entries = (E[]) new Object[bufferSize];
    }

    private int ceilingNextPowerOfTwo(final int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
    }

    @Override
    public E remove() {
        E x = poll();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    @Override
    public E poll() {
        long h = headAfterWirte.get();
        long t = tail.get();
        while (t < h) {
            if (tail.compareAndSet(t, t + 1)) {
                return entries[(int) (t & indexMask)];
            }
            h = headAfterWirte.get();
            t = tail.get();
        }
        return null;
    }

    @Override
    public E element() {
        E x = peek();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    @Override
    public E peek() {
        long h = headAfterWirte.get();
        long t = tail.get();
        if (t < h) {
            return entries[(int) (t & indexMask)];
        }
        return null;
    }

    @Override
    public boolean add(E e) {
        if (offer(e))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        long h = headAfterWirte.get();
        long t = tail.get();
        while (h - t < capacity) {
            if (headBeforeWirte.compareAndSet(h, h + 1)) {
                entries[(int) (h & indexMask)] = e;
                headAfterWirte.incrementAndGet();
                return true;
            }
            h = headAfterWirte.get();
            t = tail.get();
        }
        return false;
    }

    protected void waitFor(long expiresTime) throws InterruptedException {
        // busy spin
    }

    protected void waitFor() throws InterruptedException {
        // busy spin
    }

    @Override
    public void put(E e) throws InterruptedException {
        while (!offer(e)) {
            waitFor();
        }
    }

    @Override
    public E take() throws InterruptedException {
        E t = poll();
        while (t == null) {
            waitFor();
            t = poll();
        }
        return t;
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        long expiresTime = System.nanoTime() + unit.toNanos(timeout);
        while (expiresTime < System.nanoTime()) {
            if (offer(e)) {
                return true;
            }
            waitFor(expiresTime);
        }
        return false;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long expiresTime = System.nanoTime() + unit.toNanos(timeout);
        E t = poll();
        while (expiresTime < System.nanoTime()) {
            if (t != null) {
                break;
            }
            waitFor(expiresTime);
            t = poll();
        }
        return t;
    }

    @Override
    public int remainingCapacity() {
        return capacity - size();
    }

    @Override
    public int size() {
        long t = tail.get();
        return (int) (headAfterWirte.get() - t);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        tail.set(headAfterWirte.get());
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        for (long t = tail.get(), h = headAfterWirte.get(); t < h; t++) {
            if (entries[(int) (t & indexMask)].equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new UnsupportedOperationException();
    }
}
