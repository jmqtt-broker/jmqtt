
package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;

public interface SessionMapper {

    @Select("select broker_id, client_id,state,offline_time, property, version from jmqtt_session where client_id = #{clientId}")
    SessionDO getSession(String clientId);

    @Insert("<script>" +
            "<if test=\"'${dbType}' == 'oracle'\">" +
            "MERGE INTO JMQTT_SESSION a " +
            "USING (SELECT #{id} AS ID, #{brokerId} AS BROKER_ID, #{clientId} AS CLIENT_ID, #{state} AS STATE, " +
            "#{offlineTime} AS OFFLINE_TIME, #{property} AS PROPERTY, #{version} AS VERSION FROM DUAL) b " +
            "ON (a.CLIENT_ID = b.CLIENT_ID) " +
            "WHEN MATCHED THEN " +
            "UPDATE SET a.STATE = b.STATE, a.BROKER_ID = b.BROKER_ID, a.OFFLINE_TIME = b.OFFLINE_TIME, a.PROPERTY = b.PROPERTY, a.VERSION = b.VERSION " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (ID, BROKER_ID, CLIENT_ID, STATE, OFFLINE_TIME, PROPERTY, VERSION) VALUES (b.ID, b.BROKER_ID, b.CLIENT_ID, b.STATE, b.OFFLINE_TIME, b.PROPERTY, b.VERSION)" +
            "</if>" +
            "<if test=\"'${dbType}' != 'oracle'\">" +
            "insert into jmqtt_session(id,broker_id,client_id,state,offline_time, property, version) values " +
            "(#{id},#{brokerId},#{clientId},#{state},#{offlineTime}, #{property}, #{version}) " +
            "<if test=\"'${dbType}' == 'mysql'\">" +
            "on DUPLICATE key update broker_id = #{brokerId}, state = #{state},offline_time = #{offlineTime},property=#{property},version=#{version}" +
            "</if>" +
            "<if test=\"'${dbType}' == 'postgresql'\">" +
            "on conflict(client_id) do update set broker_id = #{brokerId},state = #{state},offline_time = #{offlineTime},property=#{property},version=#{version}" +
            "</if>" +
            "</if>" +
            "</script>"
    )
    Long storeSession(SessionDO sessionDO);

}
