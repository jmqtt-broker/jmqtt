package com.jmqtt.core.mqtt.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionSyncDTO {

    private Collection<Subscription> subscriptions;

}
