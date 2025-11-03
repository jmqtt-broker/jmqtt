package org.jmqtt.broker.common.future;

import org.jmqtt.broker.store.local.model.TimerDO;

import java.util.List;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2025/11/2 13:33
 */
public class PersistHelper {

    private final static TaskPersist TASK_PERSIST;

    static {
        TASK_PERSIST = new DBTaskPersist();
    }

    public static void store(TimerDO timer) {
        TASK_PERSIST.store(timer);
    }

    public static void del(String taskId) {
        TASK_PERSIST.del(taskId);
    }

    public static List<TimerDO> getAll() {
        return TASK_PERSIST.getAll();
    }

}
