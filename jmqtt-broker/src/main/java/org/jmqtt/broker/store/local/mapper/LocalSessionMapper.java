
package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import tk.mybatis.mapper.common.Mapper;

import java.util.Collection;
import java.util.List;

public interface LocalSessionMapper extends Mapper<SessionDO> {

    @Select("select broker_id, client_id,state,online_time,offline_time, property, version, address, clean_start, keepalive from jmqtt_session where client_id = #{clientId}")
    SessionDO getSession(String clientId);

    @Select("<script> select broker_id, client_id,state,online_time,offline_time, property, version, address, clean_start, keepalive from jmqtt_session where client_id in " +
            "<foreach open=\"(\" separator=\", \" close=\")\" collection=\"clientIds\" item=\"clientId\" >" +
            "#{clientId,jdbcType=VARCHAR} </foreach></script>")
    List<SessionDO> getSessionList(@Param("clientIds") Collection<String> clientIds);

    @Insert("<script>" +
            "insert into jmqtt_session(id,broker_id,client_id,state,online_time,offline_time, property, version, address, clean_start, keepalive) values " +
            "(#{id},#{brokerId},#{clientId},#{state},#{onlineTime},#{offlineTime}, #{property}, #{version}, #{address}, #{cleanStart}, #{keepalive}) " +
            "on DUPLICATE key update " +
            "<if test=\"brokerId != null\">broker_id=#{brokerId},</if>" +
            "<if test=\"state != null\">state=#{state},</if>" +
            "<if test=\"onlineTime != null\">online_time=#{onlineTime},</if>" +
            "<if test=\"offlineTime != null\">offline_time = #{offlineTime},</if>" +
            "<if test=\"property != null\">property = #{property},</if>" +
            "<if test=\"version != null\">version = #{version},</if>" +
            "<if test=\"address != null\">address = #{address},</if>" +
            "<if test=\"cleanStart != null\">clean_start=#{cleanStart},</if>" +
            "<if test=\"keepalive != null\">keepalive=#{keepalive}</if>" +
            "</script>"
    )
    Long storeSession(SessionDO sessionDO);

}
