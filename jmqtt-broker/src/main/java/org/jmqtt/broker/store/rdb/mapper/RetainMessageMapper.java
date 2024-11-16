package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.RetainMessageDO;

import java.util.List;

public interface RetainMessageMapper {

    @Insert("<script>" +
            "INSERT INTO jmqtt_retain_message(id,topic,content) VALUES(#{id},#{topic},#{content})"
            + "<if test=\"'${dbType}' == 'mysql'\">"
            + " on DUPLICATE key update content = #{content}"
            + "</if>"
            + "<if test=\"'${dbType}' == 'postgresql'\">"
            + "on conflict(topic) do update set content = #{content}"
            + "</if>"
            + "</script>"
    )
    Long storeRetainMessage(RetainMessageDO retainMessageDO);

    @Select("SELECT id,topic,content FROM jmqtt_retain_message")
    List<RetainMessageDO> getAllRetainMessage();

    @Delete("DELETE FROM jmqtt_retain_message WHERE topic = #{topic}")
    Integer delRetainMessage(String topic);
}
