package com.truthower.suhang.mangareader.business.blockConcurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by acorn on 2020/4/11.
 */
public class Storage<T> {
    private final BlockingQueue<T> mBlockingQueue;

    public Storage(int capacity) {
        mBlockingQueue = new ArrayBlockingQueue<>(capacity);
    }

    public T take() throws InterruptedException {
        T t = mBlockingQueue.take();
        LogUtil.i("Storage take " + t.toString());
        return t;
    }

    public void put(T t) throws InterruptedException {
        mBlockingQueue.put(t);
        LogUtil.i("Storage put " + t.toString());
    }

    public int remainingCapacity() {
        return mBlockingQueue.remainingCapacity();
    }

    public boolean isEmpty() {
        return mBlockingQueue.isEmpty();
    }
}
