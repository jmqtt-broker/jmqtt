package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.WillMessageDO;

public interface WillMessageMapper {

    @Insert("<script>" +
            "INSERT INTO jmqtt_will_message(id, client_id,content,gmt_create) VALUES(#{id},#{clientId},#{content},#{gmtCreate})"
            + "<if test=\"'${dbType}' == 'mysql'\">"
            + " on DUPLICATE key update content = #{content},gmt_create = #{gmtCreate}"
            + "</if>"
            + "<if test=\"'${dbType}' == 'postgresql'\">"
            + "on conflict(client_id) do update set content = #{content},gmt_create = #{gmtCreate}"
            + "</if>"
            + "</script>"
    )
    Long storeWillMessage(WillMessageDO willMessageDO);

    @Select("SELECT id,client_id,content,gmt_create FROM jmqtt_will_message WHERE client_id = #{clientId}")
    WillMessageDO getWillMessage(String clientId);

    @Delete("DELETE FROM jmqtt_will_message WHERE client_id = #{clientId}")
    Integer delWillMessage(String clientId);

}
