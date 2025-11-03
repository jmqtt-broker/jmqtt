package org.jmqtt.broker.common.future;

import org.jmqtt.broker.store.local.LocalDB;
import org.jmqtt.broker.store.local.mapper.LocalScheduleTaskMapper;
import org.jmqtt.broker.store.local.model.TimerDO;

import java.util.List;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2025/11/2 9:58
 */
public class DBTaskPersist implements TaskPersist {

    @Override
    public void store(TimerDO timer) {
        LocalDB.getInstance().operate(session ->
                session.getMapper(LocalScheduleTaskMapper.class).storeTask(timer));
    }

    @Override
    public void del(String taskId) {
        LocalDB.getInstance().operate(session ->
                session.getMapper(LocalScheduleTaskMapper.class).del(taskId));
    }

    @Override
    public List<TimerDO> getAll() {
        return LocalDB.getInstance().operate(session ->
                session.getMapper(LocalScheduleTaskMapper.class).getAll());
    }
}
