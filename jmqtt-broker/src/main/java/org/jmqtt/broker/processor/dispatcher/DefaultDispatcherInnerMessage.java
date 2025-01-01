package org.jmqtt.broker.processor.dispatcher;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.jmqtt.broker.common.helper.RejectHandler;
import org.jmqtt.broker.common.helper.ThreadFactoryImpl;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.common.model.SubscriptionOption;
import org.jmqtt.broker.processor.HighPerformanceMessageHandler;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * 默认的消息分发实现类
 */
public class DefaultDispatcherInnerMessage extends HighPerformanceMessageHandler implements InnerMessageDispatcher {

    private static final Logger log = JmqttLogger.messageTraceLog;
    private boolean stoped = false;
    private static final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>(100000);
    private ThreadPoolExecutor pollThread;
    private int pollThreadNum;
    private SubscriptionMatcher subscriptionMatcher;
    private SessionStore sessionStore;
    private MessageStore messageStore;
    private ClusterEventHandler clusterEventHandler;


    public DefaultDispatcherInnerMessage(boolean highPerformance, SessionStore sessionStore, MessageStore messageStore,
                                         int pollThreadNum, SubscriptionMatcher subscriptionMatcher,
                                         ClusterEventHandler clusterEventHandler) {
        super(highPerformance, sessionStore);
        this.pollThreadNum = pollThreadNum;
        this.subscriptionMatcher = subscriptionMatcher;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.clusterEventHandler = clusterEventHandler;
    }

    @Override
    public void start() {
        this.pollThread = new ThreadPoolExecutor(pollThreadNum,
                pollThreadNum,
                60 * 1000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(100000),
                new ThreadFactoryImpl("pollMessage2Subscriber"),
                new RejectHandler("pollMessage", 100000));

        new Thread(() -> {
            int waitTime = 1000;
            while (!stoped) {
                try {
                    List<Message> messageList = new ArrayList<>(32);
                    Message message;
                    for (int i = 0; i < 32; i++) {
                        if (i == 0) {
                            message = messageQueue.poll(waitTime, TimeUnit.MILLISECONDS);
                        } else {
                            message = messageQueue.poll();
                        }
                        if (Objects.nonNull(message)) {
                            messageList.add(message);
                        } else {
                            break;
                        }
                    }
                    if (messageList.size() > 0) {
                        AsyncDispatcher dispatcher = new AsyncDispatcher(messageList);
                        pollThread.submit(dispatcher).get();
                    }
                } catch (InterruptedException e) {
                    LogUtil.warn(log, "poll message wrong.");
                } catch (ExecutionException e) {
                    LogUtil.warn(log, "AsyncDispatcher get() wrong.");
                }
            }
        }).start();
    }

    @Override
    public boolean appendMessage(Message message) {
        boolean isNotFull = messageQueue.offer(message);
        if (!isNotFull) {
            LogUtil.warn(log, "[PubMessage] -> the buffer queue is full");
        }
        return isNotFull;
    }

    @Override
    public void shutdown() {
        this.stoped = true;
        this.pollThread.shutdown();
    }

    class AsyncDispatcher implements Runnable {

        private List<Message> messages;

        AsyncDispatcher(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public void run() {
            if (Objects.nonNull(messages)) {
                try {
                    for (Message message : messages) {
                        String pubClientId = message.getClientId();
                        String topic = TopicAliasManager.getRealTopic(message);
                        Set<Subscription> subscriptions = subscriptionMatcher.match(topic, pubClientId);
                        for (Subscription subscription : subscriptions) {
                            String subClientId = subscription.getClientId();
                            if (ConnectManager.getInstance().containClient(subClientId)) {
                                ClientSession clientSession = ConnectManager.getInstance().getClient(subClientId);
                                if (clientSession.isMqtt5() && checkPackageSize(clientSession, (Integer) message.getHeader(MessageHeader.REMAINING_LENGTH))) {
                                    log.warn("exceeding message, clientId: {}, stop publish.", subClientId);
                                    continue;
                                }
                                SubscriptionOption option = subscription.getOption();
                                if (option != null) {
                                    if (option.isNoLocal() && pubClientId.equals(subClientId)) {
                                        continue;
                                    }
                                }
                                int qos = MessageUtil.getMinQos((int) message.getHeader(MessageHeader.QOS), subscription.getQos());
                                int messageId = clientSession.generateMessageId();
                                message.putHeader(MessageHeader.QOS, qos);
                                message.setMsgId(messageId);
                                if (qos > 0) {
                                    cacheOutflowMsg(subClientId, message);
                                }
                                if (clientSession.isMqtt5()) {
                                    if (message.validity()) {
                                        if (message.alive() > 0) {
                                            write(clientSession, subscription, message);
                                        } else {
                                            LogUtil.warn(log, "message expired. {}", JSON.toJSONString(message));
                                            // 如果是保留消息则删除
                                            Optional.ofNullable(message.getHeader(MessageHeader.RETAIN)).ifPresent(r -> {
                                                if ((boolean) r) {
                                                    messageStore.clearRetainMessage(topic);
                                                }
                                            });
                                        }
                                    } else {
                                        write(clientSession, subscription, message);
                                    }
                                } else {
                                    write(clientSession, subscription, message);
                                }
                            } else {
                                subscriptionMatcher.unSubscribe(subscription.getTopic(), subClientId);
                            }
                        }
                    }
                } catch (Exception ex) {
                    LogUtil.warn(log, "Dispatcher message failure,cause={}", ex);
                }
            }
        }
    }

    private void write(ClientSession session, Subscription subscription, Message message) {
        if (session.getCtx().channel().isWritable()) {
            MqttPublishMessage publishMessage = MessageUtil.getPubMessage(message, false, subscription.getOption());
            session.getCtx().writeAndFlush(publishMessage);
        } else {
            sessionStore.storeOfflineMsg(subscription.getClientId(), message);
        }
    }

    private boolean checkPackageSize(ClientSession clientSession, int remainingLength) {
        String clientId = clientSession.getClientId();
        Integer receiveMaximum = (Integer) sessionStore.getClientProperty(clientId, MqttProperties.MqttPropertyType.RECEIVE_MAXIMUM.value());
        return receiveMaximum != null && (remainingLength + 5) > receiveMaximum;
    }
}
