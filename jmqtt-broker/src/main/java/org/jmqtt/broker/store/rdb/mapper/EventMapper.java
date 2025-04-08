package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.EventDO;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface EventMapper extends Mapper<EventDO> {

    @Insert("insert into jmqtt_event (id, content,gmt_create,jmqtt_ip,event_code) values "
            + "(#{id},#{content},#{gmtCreate},#{jmqttIp},#{eventCode})")
    Long sendEvent(EventDO eventDO);


    @Select("<script>"
            + "select id,content,gmt_create,jmqtt_ip,event_code from jmqtt_event "
            + "<if test=\"'${dbType}' != 'oracle'\">"
            + "where id > #{offset} order by id limit #{maxNum}"
            + "</if>"
            + "<if test=\"'${dbType}' == 'oracle'\">"
            + "where id > #{offset} and rownum &lt;= #{maxNum} order by id"
            + "</if>"
            + "</script>"
    )
    List<EventDO> consumeEvent(@Param("offset") long offset, @Param("maxNum") int maxNum);

    @Select("SELECT max(id) FROM jmqtt_event")
    Long getMaxOffset();
}
