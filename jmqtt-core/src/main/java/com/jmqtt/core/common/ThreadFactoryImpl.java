package com.jmqtt.core.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryImpl implements ThreadFactory {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final String threadName;
    private final boolean virtualThread;

    public ThreadFactoryImpl(String threadName) {
        this(threadName, false);
    }

    public ThreadFactoryImpl(String threadName, boolean virtualThread) {
        this.threadName = threadName;
        this.virtualThread = virtualThread;
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = threadName + "_" + this.counter.incrementAndGet();
        if (virtualThread) {
            return Thread.ofVirtual().name(name).unstarted(r);
        }
        return new Thread(r, name);
    }
}
