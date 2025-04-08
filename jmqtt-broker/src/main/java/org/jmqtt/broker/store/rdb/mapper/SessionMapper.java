
package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import tk.mybatis.mapper.common.Mapper;

public interface SessionMapper extends Mapper<SessionDO> {

    @Select("select id, broker_id, client_id,state,online_time,offline_time, property, version, address from jmqtt_session where client_id = #{clientId}")
    SessionDO getSession(String clientId);

    @Insert("<script>" +
            "<if test=\"'${dbType}' == 'oracle'\">" +
            "MERGE INTO JMQTT_SESSION a " +
            "USING (SELECT #{id} AS ID, #{brokerId} AS BROKER_ID, #{clientId} AS CLIENT_ID, #{state} AS STATE, #{onlineTime} AS ONLINE_TIME, " +
            "#{offlineTime} AS OFFLINE_TIME, #{property} AS PROPERTY, #{version} AS VERSION, #{address} AS ADDRESS FROM DUAL) b " +
            "ON (a.CLIENT_ID = b.CLIENT_ID) " +
            "WHEN MATCHED THEN " +
            "UPDATE SET a.STATE = b.STATE, a.BROKER_ID = b.BROKER_ID, a.ONLINE_TIME = b.ONLINE_TIME, a.OFFLINE_TIME = b.OFFLINE_TIME, a.PROPERTY = b.PROPERTY, a.VERSION = b.VERSION, a.ADDRESS = b.ADDRESS " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (ID, BROKER_ID, CLIENT_ID, STATE, ONLINE_TIME, OFFLINE_TIME, PROPERTY, VERSION, ADDRESS) VALUES (b.ID, b.BROKER_ID, b.CLIENT_ID, b.STATE, b.ONLINE_TIME, b.OFFLINE_TIME, b.PROPERTY, b.VERSION, b.ADDRESS)" +
            "</if>" +
            "<if test=\"'${dbType}' != 'oracle'\">" +
            "insert into jmqtt_session(id,broker_id,client_id,state,online_time,offline_time, property, version, address) values " +
            "(#{id},#{brokerId},#{clientId},#{state},#{onlineTime},#{offlineTime}, #{property}, #{version}, #{address}) " +
            "<if test=\"'${dbType}' == 'mysql'\">" +
            "on DUPLICATE key update broker_id = #{brokerId}, state = #{state},online_time = #{onlineTime},offline_time = #{offlineTime},property=#{property},version=#{version},address=#{address}" +
            "</if>" +
            "<if test=\"'${dbType}' == 'postgresql'\">" +
            "on conflict(client_id) do update set broker_id = #{brokerId},state = #{state},online_time = #{onlineTime},offline_time = #{offlineTime},property=#{property},version=#{version},address=#{address}" +
            "</if>" +
            "</if>" +
            "</script>"
    )
    Long storeSession(SessionDO sessionDO);

}
