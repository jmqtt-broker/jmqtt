package org.jmqtt.broker.common.model;

import io.netty.handler.codec.mqtt.MqttSubscriptionOption;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
