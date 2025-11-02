package org.jmqtt.broker.common.future;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.store.local.LocalDB;
import org.jmqtt.broker.store.local.mapper.LocalScheduleTaskMapper;
import org.jmqtt.broker.store.local.model.TimerDO;
import org.jmqtt.common.helper.ThreadFactoryImpl;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Slf4j
public class ScheduleTask implements TimerTask, Closeable {

    private final static HashedWheelTimer TIMER = new HashedWheelTimer(
            new ThreadFactoryImpl("JMQTT_Schedule_Task"),
            100, TimeUnit.MILLISECONDS, 512, true);

    private final String taskId;

    // 0延时，1定时
    private final TaskType type;

    // 业务类型标识，表示该定时任务所属业务
    private String bizType;

    // 任务超时时间，秒
    private Long delay;

    // 任务过期的毫秒时间戳
    private Long expireAt;

    // 任务相关的数据，从持久化恢复后可能会用到
    private Object data;

    // 是否需要持久化
    private final AtomicBoolean persist;

    // 从持久化恢复时，如果过期了，是否需要执行一次
    private Boolean exec;

    private final transient TimerTask task;

    private transient Timeout timeout;

    private final AtomicBoolean cancel = new AtomicBoolean(false);

    public ScheduleTask(String taskId, TimerTask task, long delay,
                        TaskType type) {
        this.taskId = taskId;
        this.task = task;
        this.delay = delay;
        this.expireAt = System.currentTimeMillis() + delay * 1000;
        this.type = type;
        this.persist = new AtomicBoolean(false);
        this.exec = true;
    }

    public ScheduleTask start() {
        cancel.set(false);
        this.timeout = addTask(this, delay);
        return this;
    }

    public ScheduleTask reclock(long delay) {
        close();
        this.delay = delay;
        this.expireAt = System.currentTimeMillis() + delay * 1000;
        return start();
    }

    public void refresh() {
        reclock(delay);
    }

    public void executeImmediately() {
        if (cancel.compareAndSet(false, true) && this.timeout != null) {
            this.timeout.cancel();
            delayTask(this.taskId, this.task, 0);
        }
    }

    @Override
    public void close() {
        if (cancel.compareAndSet(false, true) && this.timeout != null) {
            this.timeout.cancel();
            if (this.persist.get()) {
                LocalDB.getInstance().operate(session ->
                        session.getMapper(LocalScheduleTaskMapper.class).del(this.taskId));
            }
        }
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (!cancel.get()) {
            this.task.run(timeout);
            if (TaskType.TIMED.equals(type)) {
                // 定时任务
                this.timeout = addTask(this, this.delay);
            } else if (this.persist.get()) {
                LocalDB.getInstance().operate(session ->
                        session.getMapper(LocalScheduleTaskMapper.class).del(this.taskId));
            }
        }
    }

    public boolean timed() {
        return this.type.equals(TaskType.TIMED);
    }

    public ScheduleTask persist(boolean persist, Object data, String bizType, boolean exec) {
        this.persist.set(persist);
        this.data = data;
        this.bizType = bizType;
        this.exec = exec;
        if (persist) {
            LocalDB.getInstance().operate(session ->
                    session.getMapper(LocalScheduleTaskMapper.class).storeTask(new TimerDO(this)));
        }
        return this;
    }

    public static Timeout addTask(TimerTask task, long dealy) {
        return TIMER.newTimeout(task, dealy, TimeUnit.SECONDS);
    }

    public static ScheduleTask delayTask(String taskId, TimerTask task, long delay) {
        return new ScheduleTask(taskId, task, delay, TaskType.DELAY).start();
    }

    public static ScheduleTask timedTask(String taskId, TimerTask task, long delay) {
        return new ScheduleTask(taskId, task, delay, TaskType.TIMED).start();
    }

    public enum TaskType {
        // 延时
        DELAY,
        // 定时
        TIMED
    }
}
