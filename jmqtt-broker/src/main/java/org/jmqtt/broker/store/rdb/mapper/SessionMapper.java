
package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;

public interface SessionMapper {

    @Select("select client_id,state,offline_time from jmqtt_session where client_id = #{clientId}")
    SessionDO getSession(String clientId);

    @Insert("<script>" +
            "<if test=\"'${dbType}' == 'oracle'\">" +
            "MERGE INTO JMQTT_SESSION a " +
            "USING (SELECT #{id} AS ID, #{clientId} AS CLIENT_ID, #{state} AS STATE, " +
            "#{offlineTime} AS OFFLINE_TIME FROM DUAL) b " +
            "ON (a.CLIENT_ID = b.CLIENT_ID) " +
            "WHEN MATCHED THEN " +
            "UPDATE SET a.STATE = b.STATE, a.OFFLINE_TIME = b.OFFLINE_TIME " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (ID, CLIENT_ID, STATE, OFFLINE_TIME) VALUES (b.ID, b.CLIENT_ID, b.STATE, b.OFFLINE_TIME)" +
            "</if>" +
            "<if test=\"'${dbType}' != 'oracle'\">" +
            "insert into jmqtt_session(id,client_id,state,offline_time) values " +
            "(#{id},#{clientId},#{state},#{offlineTime}) " +
            "<if test=\"'${dbType}' == 'mysql'\">" +
            "on DUPLICATE key update state = #{state},offline_time = #{offlineTime}" +
            "</if>" +
            "<if test=\"'${dbType}' == 'postgresql'\">" +
            "on conflict(client_id) do update set state = #{state},offline_time = #{offlineTime}" +
            "</if>" +
            "</if>" +
            "</script>"
    )
    Long storeSession(SessionDO sessionDO);

}
