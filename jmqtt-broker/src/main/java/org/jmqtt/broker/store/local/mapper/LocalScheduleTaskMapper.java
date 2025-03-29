
package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.local.model.TimerDO;

import java.util.List;

public interface LocalScheduleTaskMapper {

    @Select("select timer_id, type, expire_at, expire, data, cycle, exec from schedule_task where timer_id = #{timerId} and type = #{type}")
    TimerDO getTask(@Param("timerId") String timerId, @Param("type") String type);

    @Select("select timer_id, type, expire_at, expire, data, cycle, exec from schedule_task")
    List<TimerDO> getAll();

    @Insert(
            "insert into schedule_task(timer_id, type, expire_at, expire, data, cycle, exec) values " +
                    "(#{timerId}, #{type}, #{expireAt}, #{expire}, #{data}, #{cycle}, #{exec}) " +
                    "on DUPLICATE key update expire_at = #{expireAt},expire = #{expire},data=#{data}" +
                    ",cycle=#{cycle},exec=#{exec}")
    Long storeTask(TimerDO timerDO);

    @Delete("delete from schedule_task where timer_id = #{timerId} and type = #{type}")
    Long del(@Param("timerId") String timerId, @Param("type") String type);

}
