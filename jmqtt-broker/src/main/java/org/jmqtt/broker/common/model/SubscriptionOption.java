package org.jmqtt.broker.common.model;

import io.netty.handler.codec.mqtt.MqttSubscriptionOption;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/6 9:33
 */
@Getter
@Setter
public class SubscriptionOption {

    private int subscriptionIdentifier;
    private boolean noLocal;
    private boolean retainAsPublished;
    private int retainHandling;

    public SubscriptionOption(MqttSubscriptionOption option) {
        this.noLocal = option.isNoLocal();
        this.retainAsPublished = option.isRetainAsPublished();
        this.retainHandling = option.retainHandling().value();
    }

}
