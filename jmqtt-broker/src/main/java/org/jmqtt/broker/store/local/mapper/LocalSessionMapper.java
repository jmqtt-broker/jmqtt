
package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;

import java.util.Collection;
import java.util.List;

public interface LocalSessionMapper {

    @Select("select broker_id, client_id,state,offline_time, property, version from jmqtt_session where client_id = #{clientId}")
    SessionDO getSession(String clientId);

    @Select("<script> select broker_id, client_id,state,offline_time, property, version from jmqtt_session where client_id in " +
            "<foreach open=\"(\" separator=\", \" close=\")\" collection=\"clientIds\" item=\"clientId\" >" +
            "#{clientId,jdbcType=VARCHAR} </foreach></script>")
    List<SessionDO> getSessionList(@Param("clientIds") Collection<String> clientIds);

    @Insert(
            "insert into jmqtt_session(id,broker_id,client_id,state,offline_time, property, version) values " +
            "(#{id},#{brokerId},#{clientId},#{state},#{offlineTime}, #{property}, #{version}) " +
            "on DUPLICATE key update broker_id = #{brokerId}, state = #{state},offline_time = #{offlineTime},property=#{property},version=#{version}"
    )
    Long storeSession(SessionDO sessionDO);

}
