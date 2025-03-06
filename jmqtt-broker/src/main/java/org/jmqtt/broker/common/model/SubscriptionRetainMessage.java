package org.jmqtt.broker.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRetainMessage {

    private Collection<Message> messageList;

    private Subscription subscription;

    private Boolean subRes = true;

}
