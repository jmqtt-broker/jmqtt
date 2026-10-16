package com.jmqtt.core.mqtt.control.dispatcher;

import cn.hutool.json.JSONUtil;
import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionState;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * 消息分发
 */
@Slf4j
public class MessageDispatcher {

    private final Thread pollThread;
    private final ExecutorService messageExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private static final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>(100000);
    private final SubscriptionMatcher subscriptionMatcher;
    private final SessionStore sessionStore;
    private final MessageStore messageStore;
    private final ClusterConfig clusterConfig;
    private final BrokerConfig brokerConfig;

    public MessageDispatcher(ClusterConfig clusterConfig,
                             BrokerConfig brokerConfig,
                             SessionStore sessionStore,
                             MessageStore messageStore,
                             SubscriptionMatcher subscriptionMatcher) {
        this.clusterConfig = clusterConfig;
        this.brokerConfig = brokerConfig;
        this.subscriptionMatcher = subscriptionMatcher;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.pollThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<Message> firstBatch = new ArrayList<>(16);
                    Message message = messageQueue.take();
                    firstBatch.add(message);
                    messageQueue.drainTo(firstBatch, 15);
                    this.messageExecutor.submit(new AsyncDispatcher(firstBatch));
                    Message next = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (next != null) {
                        List<Message> secondBatch = new ArrayList<>(16);
                        secondBatch.add(next);
                        for (int i = 0; i < 15; i++) {
                            next = messageQueue.poll();
                            if (next != null) {
                                secondBatch.add(next);
                            }
                        }
                        this.messageExecutor.submit(new AsyncDispatcher(secondBatch));
                    }
                } catch (InterruptedException e) {
                    log.warn("poll message thread interrupted!");
                    Thread.currentThread().interrupt();
                }
            }
        });
        this.pollThread.start();
    }

    public boolean accept(Message message) {
        boolean success = messageQueue.offer(message);
        if (!success) {
            log.warn("[MessageDispatcher] -> the dispatcher message queue is full.");
        }
        return success;
    }

    public void stop() {
        this.pollThread.interrupt();
        this.messageExecutor.shutdown();
    }

    class AsyncDispatcher implements Runnable {

        private final List<Message> messages;

        public AsyncDispatcher(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public void run() {
            for (Message message : messages) {
                try {
                    String pubClientId = message.getClientId();
                    String topic = (String) message.getHeader(MessageHeader.TOPIC);
                    Set<Subscription> subscriptions = subscriptionMatcher.match(topic, pubClientId);
                    for (Subscription subscription : subscriptions) {
                        String targetBrokerId = subscription.getBrokerId();
                        if (clusterConfig.getCurrentNodeName().equals(targetBrokerId)) {
                            send(subscription, message);
                        } else {
                            // 订阅topic的client不在当前broker，需要把消息发送给目标所在的broker，由目标broker进行发送
                            // ClusterHelper.sendMessageToCluster(new MessageDispatchDTO(subscription, message), targetBrokerId);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("dispatcher message failure.", ex);
                }
            }
        }
    }

    public void send(Subscription subscription, Message message) {
        String subClientId = subscription.getClientId();
        if (ClientManager.containsClient(subClientId)) {
            ClientSession clientSession = ClientManager.getClient(subClientId);
            boolean mqtt5 = clientSession.isMqtt5();
            int qos = MessageUtil.getMinQos((int) message.getHeader(MessageHeader.QOS), subscription.getQos());
            int messageId = clientSession.generateMessageId();
            message.putHeader(MessageHeader.QOS, qos);
            message.setMsgId(messageId);
            if (qos > 0) {
                sessionStore.cacheOutflowMsg(subClientId, message);
            }
            if (mqtt5) {
                if (message.validity()) {
                    // 该消息具有时效性，需要判断是否过期
                    if (message.alive() > 0) {
                        write(clientSession, subscription, message);
                    } else {
                        log.warn("message expired. {}", JSONUtil.toJsonStr(message));
                        // 如果是保留消息则删除
                        Optional.ofNullable(message.getHeader(MessageHeader.RETAIN)).ifPresent(r -> {
                            if ((boolean) r) {
                                String topic = (String) message.getHeader(MessageHeader.TOPIC);
                                // CompletableFuture.runAsync(() -> ClusterHelper.clearRetainMessage(topic));
                                messageStore.clearRetainMessage(topic);
                            }
                        });
                    }
                } else {
                    // 消息未设置过期间隔，发送
                    write(clientSession, subscription, message);
                }
            } else {
                // 不是mqtt5客户端，直接发送
                write(clientSession, subscription, message);
            }
        } else {
            SessionState session = sessionStore.getSession(subscription.getClientId());
            if (!SessionState.StateEnum.ONLINE.equals(session.getState())) {
                storeOffline(subscription.getClientId(), message);
            }
        }
    }

    private void write(ClientSession session, Subscription subscription, Message message) {
        if (session.getCtx().channel().isWritable()) {
            MqttPublishMessage publishMessage = MessageUtil.getPubMessage(message, false, subscription.getOption(), subscription.getClientId());
            session.getCtx().writeAndFlush(publishMessage);
        } else {
            if (!session.isCleanStart()) {
                storeOffline(subscription.getClientId(), message);
            }
        }
    }

    private void storeOffline(String clientId, Message message) {
        sessionStore.storeOfflineMsg(clientId, message);
    }

}
