
package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;

public interface LocalSessionMapper {

    @Select("select broker_id, client_id,state,offline_time, property, version from jmqtt_session where client_id = #{clientId}")
    SessionDO getSession(String clientId);

    @Insert(
            "insert into jmqtt_session(id,broker_id,client_id,state,offline_time, property, version) values " +
            "(#{id},#{brokerId},#{clientId},#{state},#{offlineTime}, #{property}, #{version}) " +
            "on DUPLICATE key update broker_id = #{brokerId}, state = #{state},offline_time = #{offlineTime},property=#{property},version=#{version}"
    )
    Long storeSession(SessionDO sessionDO);

}
