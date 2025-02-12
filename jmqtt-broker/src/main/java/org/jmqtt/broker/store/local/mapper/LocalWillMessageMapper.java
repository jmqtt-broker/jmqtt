package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.WillMessageDO;

public interface LocalWillMessageMapper {

    @Insert("INSERT INTO jmqtt_will_message(id,client_id,content,gmt_create) VALUES(#{id},#{clientId},#{content},#{gmtCreate})" +
            " on DUPLICATE key update content = #{content},gmt_create = #{gmtCreate}")
    Long storeWillMessage(WillMessageDO willMessageDO);

    @Select("SELECT id,client_id,content,gmt_create FROM jmqtt_will_message WHERE client_id = #{clientId}")
    WillMessageDO getWillMessage(String clientId);

    @Delete("DELETE FROM jmqtt_will_message WHERE client_id = #{clientId}")
    Integer delWillMessage(String clientId);
}
