package org.jmqtt.broker.common.future;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@EqualsAndHashCode(of = {"identify"})
public class JmqttFuture<T> implements Future<T> {

    // 默认过期时间
    private final static int DEFAULT_EXPIRE_SEC = 10;

    private final String identify;

    private transient Listener<T> listener;

    private transient Listener<Void> expireListener;

    private final long expire;

    private final transient CompletableFuture<T> future;

    private transient ScheduleTask task;

    private AtomicBoolean timed = new AtomicBoolean(false);

    public JmqttFuture(String identify) {
        this(identify, DEFAULT_EXPIRE_SEC);
    }

    public JmqttFuture(String identify, int expire) {
        this.identify = identify;
        this.expire = expire;
        this.future = new CompletableFuture<>();
    }

    public JmqttFuture<T> addListener(Listener<T> listener) {
        this.listener = listener;
        futureTimer();
        return this;
    }

    private void futureTimer() {
        if (timed.compareAndSet(false, true)) {
            this.task = new ScheduleTask(identify, timeout -> this.expired(),
                    expire, ScheduleTask.TaskType.DELAY).start();
        }
    }

    public JmqttFuture<T> addExpireListener(Listener<Void> expireListener) {
        this.expireListener = expireListener;
        return this;
    }

    public String identify() {
        return identify;
    }

    public long getExpire() {
        return expire;
    }

    public void expired() {
        if (!this.future.isCancelled()) {
            log.warn("request expired！");
            this.future.cancel(true);
            if (this.expireListener != null) {
                this.expireListener.handle(null);
            }
        }
    }

    public void complete(T result) {
        if (!this.future.isCancelled()) {
            if (listener != null) {
                listener.handle(result);
            }
            if (task != null) {
                task.close();
            }
            this.future.complete(result);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        throw new RuntimeException("do not invoke this method!");
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        futureTimer();
        return this.future.get(timeout, unit);
    }

    public T get(long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        futureTimer();
        return this.future.get(timeout, TimeUnit.SECONDS);
    }

}
