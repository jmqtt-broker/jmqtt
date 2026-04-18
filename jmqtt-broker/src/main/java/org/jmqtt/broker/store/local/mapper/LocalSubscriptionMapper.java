package org.jmqtt.broker.store.local.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.broker.store.rdb.daoobject.SubscriptionDO;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface LocalSubscriptionMapper extends Mapper<SubscriptionDO> {

    @Insert("INSERT INTO jmqtt_subscription(id, broker_id, client_id,topic,qos, sub_res, opt) " +
            "VALUES(#{id},#{brokerId},#{clientId},#{topic},#{qos},#{subRes},#{opt})" +
            " on DUPLICATE key update qos = #{qos},opt = #{opt}")
    Long storeSubscription(SubscriptionDO subscriptionDO);

    @Delete("DELETE FROM jmqtt_subscription WHERE client_id = #{clientId}")
    Integer clearSubscription(String clientId);

    @Delete("DELETE FROM jmqtt_subscription WHERE client_id = #{clientId} AND topic = #{topic}")
    Integer delSubscription(@Param("clientId") String clientId, @Param("topic") String topic);

    @Select("SELECT broker_id, client_id,topic,qos, sub_res, opt FROM jmqtt_subscription WHERE client_id = #{clientId}")
    List<SubscriptionDO> querySubscription(String clientId);

    @Select("SELECT broker_id, client_id,topic,qos, sub_res, opt FROM jmqtt_subscription " +
            "WHERE client_id = #{clientId} and topic = #{topic}")
    SubscriptionDO queryOneSubscription(@Param("clientId") String clientId, @Param("topic") String topic);

    @Select("SELECT broker_id, client_id,topic,qos, sub_res,opt FROM jmqtt_subscription ")
    List<SubscriptionDO> queryAllSubscription();
}
