package org.jmqtt.broker.store.rdb.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SubscriptionDO;

import java.util.List;

public interface SubscriptionMapper {

    @Insert("<script>" +
            "<if test=\"'${dbType}' == 'oracle'\">" +
            "MERGE INTO JMQTT_SUBSCRIPTION a " +
            "USING (SELECT #{id} AS ID, #{clientId} AS CLIENT_ID, #{topic} AS TOPIC," +
            " #{qos} AS QOS, #{opt} AS OPT FROM DUAL) b " +
            "ON (a.CLIENT_ID = b.CLIENT_ID AND a.TOPIC = b.TOPIC) " +
            "WHEN MATCHED THEN " +
            "UPDATE SET QOS = b.QOS, OPT = b.OPT " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (ID, CLIENT_ID, TOPIC, QOS, OPT) VALUES " +
            "(b.ID, b.CLIENT_ID, b.TOPIC, b.QOS, b.OPT)" +
            "</if>" +
            "<if test=\"'${dbType}' != 'oracle'\">" +
            "INSERT INTO jmqtt_subscription(id, client_id,topic,qos,opt) VALUES(#{id},#{clientId},#{topic},#{qos},#{opt})" +
            "<if test=\"'${dbType}' == 'mysql'\">" +
            "  on DUPLICATE key update qos = #{qos},opt = #{opt}" +
            "</if>" +
            "<if test=\"'${dbType}' == 'postgresql'\">" +
            "on conflict(client_id, topic) do update set qos = #{qos},opt = #{opt}" +
            "</if>" +
            "</if>" +
            "</script>"
    )
    Long storeSubscription(SubscriptionDO subscriptionDO);

    @Delete("DELETE FROM jmqtt_subscription WHERE client_id = #{clientId}")
    Integer clearSubscription(String clientId);

    @Delete("DELETE FROM jmqtt_subscription WHERE client_id = #{clientId} AND topic = #{topic}")
    Integer delSubscription(@Param("clientId") String clientId,@Param("topic") String topic);

    @Select("SELECT client_id,topic,qos,opt FROM jmqtt_subscription WHERE client_id = #{clientId}")
    List<SubscriptionDO> querySubscription(String clientId);
}
