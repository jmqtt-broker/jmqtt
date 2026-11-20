package com.jmqtt.core.cluster;

import com.hazelcast.collection.IQueue;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.MessageListener;
import com.jmqtt.core.cluster.handler.*;
import com.jmqtt.core.common.Control;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClusterMessageDispatcher implements Control {

    private final String localBrokerId;

    /**
     * 广播主题，接收集群广播消息
     */
    private final ITopic<ClusterMessage<?>> broadcastTopic;

    /**
     * 本地节点的队列，接收其他节点发送给自身节点的消息
     */
    private final IQueue<ClusterMessage<?>> unicastQueue;

    private final ExecutorService clusterMessageExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<ClusterMessage.EventType, ClusterMessageHandler<?>> handlers = new HashMap<>();

    private Thread pollThread;

    public ClusterMessageDispatcher(String localBrokerId, ITopic<ClusterMessage<?>> broadcastTopic, IQueue<ClusterMessage<?>> selfQueue) {
        this.localBrokerId = localBrokerId;
        this.broadcastTopic = broadcastTopic;
        this.unicastQueue = selfQueue;
        start();
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
                    clusterMessageExecutor.submit(() -> handler.handle((ClusterMessage) msg));
                }
            }
        };
        broadcastTopic.addMessageListener(listener);
    }

    private void registerUnicastListener() {
        pollThread = Thread.startVirtualThread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<ClusterMessage<?>> firstBatch = new ArrayList<>(16);
                    ClusterMessage<?> msg = unicastQueue.take();
                    firstBatch.add(msg);
                    unicastQueue.drainTo(firstBatch, 15);
                    clusterMessageExecutor.submit(() -> {
                        firstBatch.forEach(m -> {
                            ClusterMessageHandler<?> handler = handlers.get(msg.getEventType());
                            handler.handle((ClusterMessage) msg);
                        });
                    });
                    ClusterMessage<?> next = unicastQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (next != null) {
                        List<ClusterMessage<?>> batch = new ArrayList<>(16);
                        batch.add(next);
                        for (int i = 0; i < 15; i++) {
                            next = unicastQueue.poll();
                            if (next != null) {
                                batch.add(next);
                            }
                        }
                        clusterMessageExecutor.submit(() -> {
                            batch.forEach(m -> {
                                ClusterMessageHandler<?> handler = handlers.get(msg.getEventType());
                                handler.handle((ClusterMessage) msg);
                            });
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.debug("Unicast queue consumer exited.");
                    break;
                } catch (Exception e) {
                    // log.error("Unicast queue consumer error: ", e);
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

    @Override
    public void start() {
        registerHandler();
        registerBroadcastListener();
        registerUnicastListener();
    }

    @Override
    public void stop() {
        if (pollThread != null) {
            pollThread.interrupt();
        }
        handlers.clear();
        try {
            unicastQueue.destroy();
        } catch (Exception ignored) {
        }
    }
}
