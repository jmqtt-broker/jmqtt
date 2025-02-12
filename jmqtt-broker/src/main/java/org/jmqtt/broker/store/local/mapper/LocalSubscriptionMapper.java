package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SubscriptionDO;

import java.util.List;

public interface LocalSubscriptionMapper {

    @Insert("INSERT INTO jmqtt_subscription(id, client_id,topic,qos,opt) " +
            "VALUES(#{id},#{clientId},#{topic},#{qos},#{opt})" +
            " on DUPLICATE key update qos = #{qos},opt = #{opt}")
    Long storeSubscription(SubscriptionDO subscriptionDO);

    @Delete("DELETE FROM jmqtt_subscription WHERE client_id = #{clientId}")
    Integer clearSubscription(String clientId);

    @Delete("DELETE FROM jmqtt_subscription WHERE client_id = #{clientId} AND topic = #{topic}")
    Integer delSubscription(@Param("clientId") String clientId,@Param("topic") String topic);

    @Select("SELECT client_id,topic,qos,opt FROM jmqtt_subscription WHERE client_id = #{clientId}")
    List<SubscriptionDO> querySubscription(String clientId);
}
