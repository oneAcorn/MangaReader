package com.truthower.suhang.mangareader.business.blockConcurrent;

/**
 * Created by acorn on 2020/4/11.
 */
public abstract class Producer<T> implements Runnable {
    private final Storage<T> mStorage;

    public Producer(Storage<T> storage) {
        mStorage = storage;
    }

    @Override
    public void run() {
        try {
            execute(mStorage);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LogUtil.e(e.getMessage());
        }
    }

    protected abstract void execute(Storage<T> storage) throws InterruptedException;

}
