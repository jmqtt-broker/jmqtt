package com.jmqtt.core.mqtt.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.jmqtt.core.mqtt.store.persist.po.SubscriptionDO;
import lombok.*;

import java.util.Optional;

/**
 * 订阅关系
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = {"clientId", "topic"})
public class Subscription {
    private String brokerId;
    private String clientId;
    private int qos;
    private String topic;
    private Boolean subRes = true;
    private SubscriptionOption option;

    public Subscription(String clientId, String topic, int qos) {
        this.clientId = clientId;
        this.topic = topic;
        this.qos = qos;
    }

    public Subscription(String brokerId, String clientId, String topic, int qos, Boolean subRes) {
        this.brokerId = brokerId;
        this.clientId = clientId;
        this.topic = topic;
        this.qos = qos;
        this.subRes = subRes;
    }

    public Subscription(SubscriptionDO subscriptionDO) {
        BeanUtil.copyProperties(subscriptionDO, this);
        Optional.ofNullable(subscriptionDO.getOpt()).ifPresent(opt ->
                this.option = JSONUtil.toBean(opt, SubscriptionOption.class));
    }

    public static Subscription of(SubscriptionDO subscriptionDO) {
        return new Subscription(subscriptionDO);
    }

}
