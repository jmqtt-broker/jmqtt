package org.jmqtt.broker.processor.dispatcher;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscriptionOption;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.processor.HighPerformanceMessageHandler;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.common.helper.RejectHandler;
import org.jmqtt.common.helper.ThreadFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 分发集群retain、offline等定向消息
 */
@Slf4j
public class RetainMessageDispatcherImpl extends HighPerformanceMessageHandler implements RetainMessageDispatcher {

    private boolean stoped = false;
    private static final BlockingQueue<SubscriptionRetainMessage> messageQueue = new LinkedBlockingQueue<>(100000);
    private ThreadPoolExecutor pollThread;

    public RetainMessageDispatcherImpl(boolean highPerformance, SessionStore sessionStore) {
        super(highPerformance, sessionStore);
    }

    @Override
    public void start() {
        this.pollThread = new ThreadPoolExecutor(2,
                5,
                60 * 1000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(100000),
                new ThreadFactoryImpl("DirectionalMessageDispatcher"),
                new RejectHandler("DirectionalMessage", 100000));

        new Thread(() -> {
            int waitTime = 1000;
            while (!stoped) {
                try {
                    List<SubscriptionRetainMessage> messageList = new ArrayList<>(32);
                    SubscriptionRetainMessage message;
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
    public boolean appendMessage(SubscriptionRetainMessage message) {
        boolean isNotFull = messageQueue.offer(message);
        if (!isNotFull) {
            log.warn("[DirectionalMessage] -> the buffer queue is full");
        }
        return isNotFull;
    }

    @Override
    public void shutdown() {
        this.stoped = true;
        this.pollThread.shutdown();
    }

    class AsyncDispatcher implements Runnable {

        private List<SubscriptionRetainMessage> messages;

        AsyncDispatcher(List<SubscriptionRetainMessage> messages) {
            this.messages = messages;
        }

        @Override
        public void run() {
            if (Objects.nonNull(messages)) {
                for (SubscriptionRetainMessage message : messages) {
                    try {
                        Collection<Message> retainList = message.getMessageList();
                        Subscription subscription = message.getSubscription();
                        Boolean subRes = message.getSubRes();
                        if (!retainList.isEmpty()) {
                            String clientId = subscription.getClientId();
                            ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
                            SubscriptionOption option = subscription.getOption();
                            retainList.forEach(retainMsg -> {
                                int minQos = MessageUtil.getMinQos((int) retainMsg.getHeader(MessageHeader.QOS), subscription.getQos());
                                retainMsg.putHeader(MessageHeader.QOS, minQos);
                                int messageId = clientSession.generateMessageId();
                                retainMsg.putHeader(MessageHeader.QOS, minQos);
                                retainMsg.setMsgId(messageId);
                                if (minQos > 0) {
                                    cacheOutflowMsg(clientId, retainMsg);
                                }
                                if (clientSession.isMqtt5()) {
                                    if (MqttSubscriptionOption.RetainedHandlingPolicy.SEND_AT_SUBSCRIBE.value() == option.getRetainHandling() ||
                                            (MqttSubscriptionOption.RetainedHandlingPolicy.SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS.value() == option.getRetainHandling() && subRes)) {
                                        if (retainMsg.validity()) {
                                            if (retainMsg.alive() > 0) {
                                                MqttPublishMessage publishMessage = MessageUtil.getPubMessage(retainMsg, false, option, clientId);
                                                clientSession.getCtx().writeAndFlush(publishMessage);
                                            } else {
                                                // retain消息过期了，删除
                                                log.info("[Subscribe] -> retain message expired, delete.");
                                                BrokerContext.getMessageStore().clearRetainMessage((String) retainMsg.getHeader(MessageHeader.TOPIC));
                                            }
                                        } else {
                                            MqttPublishMessage publishMessage = MessageUtil.getPubMessage(retainMsg, false, option, clientId);
                                            clientSession.getCtx().writeAndFlush(publishMessage);
                                        }
                                    }
                                } else {
                                    MqttPublishMessage publishMessage = MessageUtil.getPubMessage(retainMsg, false, option, clientId);
                                    clientSession.getCtx().writeAndFlush(publishMessage);
                                }
                            });
                        }
                    } catch (Exception ex) {
                        log.warn("Dispatcher message failure,cause: ", ex);
                    }
                }
            }
        }
    }

}
