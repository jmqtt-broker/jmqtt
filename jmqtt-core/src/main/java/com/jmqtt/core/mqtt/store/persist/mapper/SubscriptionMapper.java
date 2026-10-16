package com.jmqtt.core.mqtt.store.persist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jmqtt.core.mqtt.store.persist.po.SubscriptionDO;
import org.apache.ibatis.annotations.Insert;

public interface SubscriptionMapper extends BaseMapper<SubscriptionDO> {

    @Insert("""
            INSERT INTO jmqtt_subscription (id, broker_id, client_id, topic, qos, sub_res, opt)
            VALUES (#{id}, #{brokerId}, #{clientId}, #{topic}, #{qos}, #{subRes}, #{opt})
            ON DUPLICATE KEY UPDATE broker_id = VALUES(broker_id), qos = VALUES(qos),
                sub_res = VALUES(sub_res), opt = VALUES(opt)
            """)
    boolean upsert(SubscriptionDO subscriptionDO);

}
