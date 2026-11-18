package com.jmqtt.core.mqtt.timer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jmqtt.core.mqtt.store.persist.LocalDB;
import com.jmqtt.core.mqtt.store.persist.mapper.ScheduleTaskMapper;
import com.jmqtt.core.mqtt.store.persist.po.ScheduleTask;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class LocalDBScheduleTaskStorage implements ScheduleTaskStorage {

    @Override
    public void save(ScheduleTask task) {
        LocalDB.getInstance().execute(ScheduleTaskMapper.class, mapper -> mapper.insertOrUpdate(task));
    }

    @Override
    public ScheduleTask get(String timerId) {
        return LocalDB.getInstance().execute(ScheduleTaskMapper.class, mapper -> mapper.selectById(timerId));
    }

    @Override
    public List<ScheduleTask> loadAll() {
        List<ScheduleTask> list = LocalDB.getInstance().execute(ScheduleTaskMapper.class,
                mapper -> mapper.selectList(Wrappers.lambdaQuery(ScheduleTask.class)));
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<ScheduleTask> loadExpired() {
        long now = System.currentTimeMillis() / 1000L;
        List<ScheduleTask> list = LocalDB.getInstance().execute(ScheduleTaskMapper.class,
                mapper -> mapper.selectList(Wrappers.lambdaQuery(ScheduleTask.class)
                        .le(ScheduleTask::getExpireAt, now)));
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void remove(String timerId) {
        LocalDB.getInstance().execute(ScheduleTaskMapper.class,
                mapper -> mapper.deleteById(timerId));
    }

    @Override
    public void updateExpireAt(String timerId, long expireAt) {
        LocalDB.getInstance().execute(ScheduleTaskMapper.class, mapper -> {
            ScheduleTask task = mapper.selectById(timerId);
            if (task != null) {
                task.setExpireAt(expireAt);
                mapper.updateById(task);
            }
            return null;
        });
    }
}
