package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.InflowMessageDO;

import java.util.List;

public interface InflowMessageMapper {

    @Insert("<script>" +
            "INSERT INTO jmqtt_inflow_message(id, client_id,msg_id,content,gmt_create) VALUES(#{id},#{clientId},#{msgId},#{content},#{gmtCreate})"
            + "<if test=\"'${dbType}' == 'mysql'\">"
            + " on DUPLICATE key update content = #{content},gmt_create = #{gmtCreate}"
            + "</if>"
            + "<if test=\"'${dbType}' == 'postgresql'\">"
            + "on conflict(client_id, msg_id) do update set content = #{content},gmt_create = #{gmtCreate}"
            + "</if>"
            + "</script>"
    )
    Long cacheInflowMessage(InflowMessageDO inflowMessageDO);

    @Select("SELECT id,client_id,msg_id,content,gmt_create FROM jmqtt_inflow_message WHERE client_id = #{clientId} and msg_id = #{msgId}")
    InflowMessageDO getInflowMessage(@Param("clientId") String clientId, @Param("msgId") int msgId);

    @Delete("DELETE FROM jmqtt_inflow_message WHERE id = #{id}")
    Integer delInflowMessage(Long id);

    @Select("SELECT id,client_id,msg_id,content,gmt_create FROM jmqtt_inflow_message WHERE client_id = #{clientId} order by gmt_create asc")
    List<InflowMessageDO> getAllInflowMessage(String clientId);
}
