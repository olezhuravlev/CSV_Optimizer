package com.csvoptimizer;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EvictingQueue<E> extends LinkedBlockingQueue<E> {

    private int maxSize;

    public EvictingQueue(int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }

    @Override
    public void put(E e) throws InterruptedException {
        processSize();
        super.put(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {
        processSize();
        return super.offer(e, timeout, unit);
    }

    @Override
    public boolean offer(E e) {
        processSize();
        return super.offer(e);
    }

    private void processSize() {
        int size = size();
        if (size >= maxSize) {
            super.poll();
        }
    }
}
