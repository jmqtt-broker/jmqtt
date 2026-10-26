package com.jmqtt.core.cluster;

import com.hazelcast.collection.IQueue;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.MessageListener;
import com.jmqtt.core.cluster.handler.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ClusterMessageDispatcher {

    private final String localBrokerId;

    /**
     * 广播主题，接收集群广播消息
     */
    private final ITopic<ClusterMessage<?>> broadcastTopic;

    /**
     * 本地节点的队列，接收其他节点发送给自身节点的消息
     */
    private final IQueue<ClusterMessage<?>> unicastQueue;

    private final Map<ClusterMessage.EventType, ClusterMessageHandler<?>> handlers = new HashMap<>();

    public ClusterMessageDispatcher(String localBrokerId, ITopic<ClusterMessage<?>> broadcastTopic, IQueue<ClusterMessage<?>> selfQueue) {
        this.localBrokerId = localBrokerId;
        this.broadcastTopic = broadcastTopic;
        this.unicastQueue = selfQueue;
        registerHandler();
        registerBroadcastListener();
        registerUnicastListener();
    }

    public void registerHandler() {
        registerHandler(SessionConnectHandler.of());
        registerHandler(SessionDisconnectHandler.of());
        registerHandler(PublishMessageHandler.of());
        registerHandler(SubscriptionHandler.of());
        registerHandler(UnSubscriptionHandler.of());
        registerHandler(RetainMessageHandler.of());
    }

    private void registerHandler(ClusterMessageHandler<?> handler) {
        handlers.put(handler.getType(), handler);
    }

    private void registerBroadcastListener() {
        MessageListener<ClusterMessage<?>> listener = message -> {
            ClusterMessage<?> msg = message.getMessageObject();
            if (msg != null && !localBrokerId.equals(msg.getBrokerId())) {
                ClusterMessageHandler<?> handler = handlers.get(msg.getEventType());
                if (handler != null) {
                    handler.handle((ClusterMessage) msg);
                }
            }
        };
        broadcastTopic.addMessageListener(listener);
    }

    private void registerUnicastListener() {
        Thread.startVirtualThread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ClusterMessage<?> msg = unicastQueue.take();
                    ClusterMessageHandler<?> handler = handlers.get(msg.getEventType());
                    if (handler != null) {
                        handler.handle((ClusterMessage) msg);
                    }
                } catch (InterruptedException e) {
                    log.warn("Unicast message queue error.", e);
                }
            }
        });
    }

    public void broadcast(ClusterMessage<?> message) {
        broadcastTopic.publish(message);
    }

    public void unicast(IQueue<ClusterMessage<?>> target, ClusterMessage<?> message) {
        target.offer(message);
    }

}
