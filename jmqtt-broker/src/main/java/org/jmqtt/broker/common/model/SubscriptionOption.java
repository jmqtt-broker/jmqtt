package org.jmqtt.broker.common.model;

import io.netty.handler.codec.mqtt.MqttSubscriptionOption;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/6 9:33
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class SubscriptionOption {

    private Integer subscriptionIdentifier;
    private Boolean noLocal;
    private Boolean retainAsPublished;
    private Integer retainHandling;

    public SubscriptionOption(MqttSubscriptionOption option) {
        this.noLocal = option.isNoLocal();
        this.retainAsPublished = option.isRetainAsPublished();
        this.retainHandling = option.retainHandling().value();
    }

}
