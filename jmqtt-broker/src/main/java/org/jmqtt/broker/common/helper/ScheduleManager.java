package org.jmqtt.broker.common.helper;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.store.local.LocalDB;
import org.jmqtt.broker.store.local.mapper.LocalScheduleTaskMapper;
import org.jmqtt.broker.store.local.model.TimerDO;
import org.jmqtt.common.helper.ThreadFactoryImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class ScheduleManager {

    private final HashedWheelTimer hashedWheelTimer;

    private final Map<TimerBO, TimerBO> timeroutCache;

    private static AtomicBoolean load = new AtomicBoolean(false);

    private static final ScheduleManager SCHEDULER = new ScheduleManager();

    private ScheduleManager() {
        this.hashedWheelTimer = new HashedWheelTimer(
                new ThreadFactoryImpl("JMQTT_Schedule_thread"),
                100,
                TimeUnit.MILLISECONDS,
                512,
                true
        );
        this.timeroutCache = new ConcurrentHashMap<>();
    }

    public static ScheduleManager getInstance() {
        if (load.compareAndSet(false, true)) {
            SCHEDULER.loadTask();
        }
        return SCHEDULER;
    }

    public void loadTask() {
        // 系统故障或重启，从本地恢复重启前的调度任务
        // TODO 不能一次查所有记录
        List<TimerDO> tasks = (List<TimerDO>) LocalDB.getInstance().operate(session ->
                session.getMapper(LocalScheduleTaskMapper.class).getAll());
        tasks.forEach(timerDO -> {
            long remain = timerDO.getExpireAt() - System.currentTimeMillis();
            Boolean exec = timerDO.getExec();
            TimerBO timerBO = new TimerBO(timerDO);
            if (remain > 0) {
                if (exec != null && exec) {
                    timerBO.setExpire((int) remain / 1000);
                    log.info("restore task: {}, {}, expire after: {}", timerBO.getType(), timerBO.getTimerId(), timerBO.getExpire());
                    addTask(timerBO);
                }
            } else {
                if (exec != null && exec) {
                    log.info("restore task: {}, {}, expired, execute callback.", timerBO.getType(), timerBO.getTimerId());
                    timerBO.process();
                    LocalDB.getInstance().operate(session ->
                            session.getMapper(LocalScheduleTaskMapper.class).del(timerBO.getTimerId(), timerBO.getType().name()));
                }
            }
        });
    }

    /**
     * 添加调度任务
     *
     * @param timerBO 任务
     */
    public void addTask(TimerBO timerBO) {
        if (!timerBO.getCycle()) {
            LocalDB.getInstance().operate(session ->
                    session.getMapper(LocalScheduleTaskMapper.class).storeTask(new TimerDO(timerBO)));
        }
        Timeout expired = this.hashedWheelTimer.newTimeout(timeout -> {
            timerBO.process();
            if (timerBO.getCycle()) {
                addTask(timerBO);
            } else {
                LocalDB.getInstance().operate(session ->
                        session.getMapper(LocalScheduleTaskMapper.class)
                                .del(timerBO.getTimerId(), timerBO.getType().name()));
            }
        }, timerBO.getExpire(), TimeUnit.SECONDS);
        timerBO.setTimeout(expired);
        timeroutCache.putIfAbsent(timerBO, timerBO);
    }

    /**
     * 添加定时任务
     * @param timerBO   任务
     */
    public void addSchedule(TimerBO timerBO) {
        log.info("start schedule task: {}, {}, expire: {}", timerBO.getType(), timerBO.getTimerId(), timerBO.getExpire());
        timerBO.setCycle(true);
        LocalDB.getInstance().operate(session ->
                session.getMapper(LocalScheduleTaskMapper.class).storeTask(new TimerDO(timerBO)));
        addTask(timerBO);
    }

    /**
     * 添加延时任务
     *
     * @param timerBO   任务
     */
    public void addDelay(TimerBO timerBO) {
        log.info("start delay task: {}, {}, expire: {}", timerBO.getType(), timerBO.getTimerId(), timerBO.getExpire());
        timerBO.setCycle(false);
        addTask(timerBO);
    }

    public void simpleDelay(Consumer<Timeout> execute, long seconds) {
        this.hashedWheelTimer.newTimeout(execute::accept, seconds, TimeUnit.SECONDS);
    }

    /**
     * 取消任务
     *
     * @param timerBO 任务
     * @return 成功失败
     */
    public boolean cancel(TimerBO timerBO) {
        TimerBO timer = this.timeroutCache.remove(timerBO);
        if (timer != null) {
            Timeout timeout = timer.getTimeout();
            if (timeout != null) {
                log.info("stop delay task:{}, {}", timerBO.getType().name(), timerBO.getTimerId());
                LocalDB.getInstance().operate(session ->
                        session.getMapper(LocalScheduleTaskMapper.class).del(timerBO.getTimerId(), timerBO.getType().name()));
                return timeout.cancel();
            }
        }
        return false;
    }

    public TimerBO getTimerData(TimerBO timerBO) {
        return this.timeroutCache.get(timerBO);
    }

}
