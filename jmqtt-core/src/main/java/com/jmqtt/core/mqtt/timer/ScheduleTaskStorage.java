package com.jmqtt.core.mqtt.timer;

import com.jmqtt.core.mqtt.store.persist.po.ScheduleTask;

import java.util.Collection;

public interface ScheduleTaskStorage {

    void save(ScheduleTask task);

    ScheduleTask get(String timerId);

    Collection<ScheduleTask> loadAll();

    Collection<ScheduleTask> loadExpired();

    void remove(String timerId);

    void updateExpireAt(String timerId, long expireAt);
}
