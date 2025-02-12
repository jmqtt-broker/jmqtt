package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.RetainMessageDO;

import java.util.List;

public interface RetainMessageMapper {

    @Insert("<script>" +
            "<if test=\"'${dbType}' == 'oracle'\">" +
            "MERGE INTO JMQTT_RETAIN_MESSAGE a " +
            "USING (SELECT #{id} AS ID, #{topic} AS TOPIC," +
            "#{content} AS CONTENT FROM DUAL) b " +
            "ON (a.TOPIC = b.TOPIC) " +
            "WHEN MATCHED THEN " +
            "UPDATE SET CONTENT = b.CONTENT " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (ID, TOPIC, CONTENT) VALUES " +
            "(b.ID, b.TOPIC, b.CONTENT)" +
            "</if>" +
            "<if test=\"'${dbType}' != 'oracle'\">" +
            "INSERT INTO jmqtt_retain_message(id,topic,content) " +
            "VALUES(#{id},#{topic},#{content})" +
            "<if test=\"'${dbType}' == 'mysql'\">" +
            " on DUPLICATE key update content = #{content}" +
            "</if>" +
            "<if test=\"'${dbType}' == 'postgresql'\">" +
            "on conflict(topic) do update set content = #{content}" +
            "</if>" +
            "</if>" +
            "</script>"
    )
    Long storeRetainMessage(RetainMessageDO retainMessageDO);

    @Select("SELECT id,topic,content FROM jmqtt_retain_message")
    List<RetainMessageDO> getAllRetainMessage();

    @Select("SELECT id,topic,content FROM jmqtt_retain_message where topic like #{topicRegx}")
    List<RetainMessageDO> getRetainMessage(String topicRegx);

    @Delete("DELETE FROM jmqtt_retain_message WHERE topic = #{topic}")
    Integer delRetainMessage(String topic);
}
