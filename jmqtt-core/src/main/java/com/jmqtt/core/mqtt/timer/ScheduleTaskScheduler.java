package com.jmqtt.core.mqtt.timer;

import com.jmqtt.core.common.Control;
import com.jmqtt.core.mqtt.store.persist.po.ScheduleTask;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ScheduleTaskScheduler implements Control {

    private final HashedWheelTimer wheelTimer;

    private final ScheduleTaskStorage storage;

    private final Map<String, Timeout> timeouts = new ConcurrentHashMap<>();

    private final Map<String, TaskHandler> handlers = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean(false);

    public ScheduleTaskScheduler() {
        this(new LocalDBScheduleTaskStorage());
    }

    public ScheduleTaskScheduler(ScheduleTaskStorage storage) {
        this.storage = storage;
        this.wheelTimer = new HashedWheelTimer(r -> {
            Thread t = new Thread(r, "jmqtt-schedule-wheel");
            t.setDaemon(true);
            return t;
        }, 150, TimeUnit.MILLISECONDS, 512);
    }

    public void registerHandler(TimerType type, TaskHandler handler) {
        handlers.put(type.name(), handler);
    }

    public void unregisterHandler(TimerType type) {
        handlers.remove(type.name());
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        log.info("ScheduleTaskScheduler starting...");
        wheelTimer.start();
        reload();
        log.info("ScheduleTaskScheduler started.");
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        log.info("ScheduleTaskScheduler stopping...");
        wheelTimer.stop();
        timeouts.clear();
        log.info("ScheduleTaskScheduler stopped.");
    }

    public void reload() {
        log.info("ScheduleTaskScheduler reloading tasks from storage...");
        storage.loadAll().forEach(this::scheduleFromStorage);
        log.info("ScheduleTaskScheduler reloaded, active tasks: {}", timeouts.size());
    }

    private void scheduleFromStorage(ScheduleTask task) {
        long now = System.currentTimeMillis() / 1000L;
        if (task.getExpireAt() <= now) {
            if (task.getExec() != null && task.getExec() == 1) {
                dispatchTask(task);
            } else {
                storage.remove(task.getTimerId());
            }
            return;
        }
        long delayMs = (task.getExpireAt() - now) * 1000L;
        schedule(task, delayMs);
    }

    public void create(String timerId, TimerType type, long delaySec, String data, boolean cycle, int cycleExecSec) {
        long expireAt = System.currentTimeMillis() / 1000L + delaySec;
        ScheduleTask task = ScheduleTask.builder()
                .timerId(timerId)
                .type(type.name())
                .expireAt(expireAt)
                .expire(cycle ? (long) cycleExecSec : delaySec)
                .data(data)
                .cycle(cycle ? 1 : 0)
                .exec(0)
                .build();
        storage.save(task);
        schedule(task, delaySec * 1000L);
    }

    public void create(String timerId, TimerType type, long delaySec, String data) {
        create(timerId, type, delaySec, data, false, 0);
    }

    public void cancel(String timerId) {
        Timeout timeout = timeouts.remove(timerId);
        if (timeout != null) {
            timeout.cancel();
            storage.remove(timerId);
            log.info("Schedule task canceled: timerId={}", timerId);
        }
    }

    public boolean exists(String timerId) {
        return timeouts.containsKey(timerId);
    }

    public void reschedule(String timerId, long newDelaySec) {
        Timeout old = timeouts.remove(timerId);
        if (old != null) {
            old.cancel();
        }
        ScheduleTask task = storage.get(timerId);
        if (task != null) {
            long newExpireAt = System.currentTimeMillis() / 1000L + newDelaySec;
            task.setExpireAt(newExpireAt);
            task.setExpire(newDelaySec);
            storage.save(task);
            schedule(task, newDelaySec * 1000L);
        }
    }

    private void schedule(ScheduleTask task, long delayMs) {
        TimerTask timerTask = timeout -> {
            timeouts.remove(task.getTimerId());
            if (timeout.isCancelled()) {
                return;
            }
            dispatchTask(task);
            if (task.getCycle() != null && task.getCycle() == 1) {
                long nextExpireAt = System.currentTimeMillis() / 1000L + task.getExpire();
                task.setExpireAt(nextExpireAt);
                storage.save(task);
                schedule(task, task.getExpire() * 1000L);
            } else {
                storage.remove(task.getTimerId());
            }
        };

        Timeout timeout = wheelTimer.newTimeout(timerTask,
                Math.max(delayMs, 100L), TimeUnit.MILLISECONDS);
        timeouts.put(task.getTimerId(), timeout);
        log.info("Schedule task added: timerId={}, type={}, delayMs={}, cycle={}",
                task.getTimerId(), task.getType(), delayMs, task.getCycle());
    }

    private void dispatchTask(ScheduleTask task) {
        TaskHandler handler = handlers.get(task.getType());
        if (handler == null) {
            log.warn("No handler registered for schedule task type: {}, timerId={}",
                    task.getType(), task.getTimerId());
            return;
        }
        try {
            handler.handle(task);
        } catch (Exception e) {
            log.error("Schedule task execution error: timerId={}, type={}",
                    task.getTimerId(), task.getType(), e);
        }
    }

    public int activeTaskCount() {
        return timeouts.size();
    }
}
