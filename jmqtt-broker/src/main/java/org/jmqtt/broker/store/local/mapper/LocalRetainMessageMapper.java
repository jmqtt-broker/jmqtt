package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.RetainMessageDO;

import java.util.List;

public interface LocalRetainMessageMapper {

    @Insert("INSERT INTO jmqtt_retain_message(id,topic,content) VALUES(#{id},#{topic},#{content})" +
            " on DUPLICATE key update content = #{content}")
    Long storeRetainMessage(RetainMessageDO retainMessageDO);

    @Select("SELECT id,topic,content FROM jmqtt_retain_message")
    List<RetainMessageDO> getAllRetainMessage();

    @Select("SELECT id,topic,content FROM jmqtt_retain_message where topic like #{topicRegx}")
    List<RetainMessageDO> getRetainMessage(String topicRegx);

    @Delete("DELETE FROM jmqtt_retain_message WHERE topic = #{topic}")
    Integer delRetainMessage(String topic);
}
