package com.smartagri.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class StripedLock {

    private static final int DEFAULT_STRIPES = 64;
    private final ReentrantLock[] locks;
    private final int mask;

    public StripedLock() {
        this(DEFAULT_STRIPES);
    }

    public StripedLock(int stripes) {
        int size = 1;
        while (size < stripes) size <<= 1;
        this.mask = size - 1;
        this.locks = new ReentrantLock[size];
        for (int i = 0; i < size; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    public boolean tryLock(String key, long timeoutMs) {
        ReentrantLock lock = lockFor(key);
        try {
            return lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void lock(String key) {
        lockFor(key).lock();
    }

    public void unlock(String key) {
        ReentrantLock lock = lockFor(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private ReentrantLock lockFor(String key) {
        int hash = (key == null ? 0 : key.hashCode());
        return locks[(hash ^ (hash >>> 16)) & mask];
    }
}
