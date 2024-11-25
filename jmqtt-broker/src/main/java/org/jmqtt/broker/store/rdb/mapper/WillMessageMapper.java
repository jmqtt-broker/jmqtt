package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.WillMessageDO;

public interface WillMessageMapper {

    @Insert("<script>" +
            "<if test=\"'${dbType}' == 'oracle'\">" +
            "MERGE INTO JMQTT_WILL_MESSAGE a " +
            "USING (SELECT #{id} AS ID, #{clientId} AS CLIENT_ID, #{content} AS CONTENT," +
            "#{gmtCreate} AS GMT_CREATE FROM DUAL) b " +
            "ON (a.CLIENT_ID = b.CLIENT_ID) " +
            "WHEN MATCHED THEN " +
            "UPDATE SET GMT_CREATE = b.GMT_CREATE " +
            "WHEN NOT MATCHED THEN " +
            "  INSERT (ID, CLIENT_ID, CONTENT, GMT_CREATE) VALUES " +
            "(b.ID, b.CLIENT_ID, b.CONTENT, b.GMT_CREATE)" +
            "</if>" +
            "<if test=\"'${dbType}' != 'oracle'\">" +
            "INSERT INTO jmqtt_will_message(id, client_id,content,gmt_create) " +
            "VALUES(#{id},#{clientId},#{content},#{gmtCreate})" +
            "<if test=\"'${dbType}' == 'mysql'\">" +
            " on DUPLICATE key update content = #{content},gmt_create = #{gmtCreate}" +
            "</if>" +
            "<if test=\"'${dbType}' == 'postgresql'\">" +
            "on conflict(client_id) do update set content = #{content},gmt_create = #{gmtCreate}" +
            "</if>" +
            "</if>" +
            "</script>"
    )
    Long storeWillMessage(WillMessageDO willMessageDO);

    @Select("SELECT id,client_id,content,gmt_create FROM jmqtt_will_message WHERE client_id = #{clientId}")
    WillMessageDO getWillMessage(String clientId);

    @Delete("DELETE FROM jmqtt_will_message WHERE client_id = #{clientId}")
    Integer delWillMessage(String clientId);

}
