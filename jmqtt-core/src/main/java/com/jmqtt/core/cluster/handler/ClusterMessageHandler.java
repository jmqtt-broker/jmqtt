package com.jmqtt.core.cluster.handler;

import com.jmqtt.core.cluster.ClusterMessage;

public interface ClusterMessageHandler<T> {

    ClusterMessage.EventType getType();

    void handle(ClusterMessage<T> msg);

}
