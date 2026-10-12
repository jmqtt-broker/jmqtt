package com.jmqtt.core.mqtt.control.recover;

import cn.hutool.core.collection.CollUtil;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class ReSendMessageService {

    private Thread thread;
    private boolean stoped = false;
    private BlockingQueue<String> clients = new LinkedBlockingQueue<>();
    private int maxSize = 10000;
    private SessionStore sessionStore;
    private MessageStore messageStore;

    private ExecutorService sendMessageExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ReSendMessageService(SessionStore sessionStore, MessageStore messageStore) {
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.thread = new Thread(new PutClient());
    }

    ;

    public boolean put(String clientId) {
        if (this.clients.size() > maxSize) {
            log.warn("ReSend message busy! the client queue size is over {}", maxSize);
            return false;
        }
        this.clients.offer(clientId);
        return true;
    }

    public void wakeUp() {
        LockSupport.unpark(thread);
    }

    public void start() {
        thread.start();
    }

    public void shutdown() {
        if (!stoped) {
            stoped = true;
        }
    }

    // TODO 临时占位，需要代码优化
    interface Build {
        MqttMessage buildMqttMessage(Message message);
    }


    public boolean dispatcherMessage(String clientId, Message message, Build build) {
        ClientSession clientSession = ClientManager.getClient(clientId);
        // client off line again
        if (clientSession == null) {
            log.warn("The client offline again, put the message to the offline queue,clientId:{}", clientId);
            return false;
        }
        MqttMessage mqttMessage = build.buildMqttMessage(message);
        clientSession.getCtx().writeAndFlush(mqttMessage);
        return true;
    }

    class ResendMessageTask implements Callable<Boolean> {

        private String clientId;

        public ResendMessageTask(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public Boolean call() {

            // 入栈报文：未处理的pubRec报文
            Collection<Message> waitPubRecMsgs = sessionStore.getAllInflowMsg(clientId);
            if (CollUtil.isNotEmpty(waitPubRecMsgs)) {
                for (Message waitPubRecMsg : waitPubRecMsgs) {
                    if (!dispatcherMessage(clientId, waitPubRecMsg, new Build() {
                        @Override
                        public MqttMessage buildMqttMessage(Message message) {
                            return MessageUtil.getPubReplyMessage(message.getMsgId(), MqttMessageType.PUBREC, (byte) 0x00, null, true);
                        }
                    })) {
                        log.warn("ReSendMessageService resend inflow error,{}", waitPubRecMsg);
                    }
                }
            }

            Build publishMqttMsg = new Build() {
                @Override
                public MqttMessage buildMqttMessage(Message message) {
                    return MessageUtil.getPubMessage(message, false);
                }
            };

            // 出栈报文-qos1,qos2第一阶段：
            Collection<Message> outflowMsgs = sessionStore.getAllOutflowMsg(clientId);
            if (CollUtil.isNotEmpty(outflowMsgs)) {
                for (Message message : outflowMsgs) {
                    if (!dispatcherMessage(clientId, message, publishMqttMsg)) {
                        log.warn("ReSendMessageService resend outflow error,{}", message);
                    }
                }
            }

            // 出栈报文-qos2第二阶段：
            Collection<Integer> qos2MsgIds = sessionStore.getAllOutflowSecMsgId(clientId);
            if (CollUtil.isNotEmpty(qos2MsgIds)) {
                for (Integer msgId : qos2MsgIds) {
                    Message temp = new Message();
                    temp.setMsgId(msgId);
                    if (!dispatcherMessage(clientId, temp, new Build() {
                        @Override
                        public MqttMessage buildMqttMessage(Message message) {
                            return MessageUtil.getPubReplyMessage(msgId, MqttMessageType.PUBREL, (byte) 0x00, null, true);
                        }
                    })) {
                        return false;
                    }
                }
            }

            /*if (!ClusterHelper.lightning()) {
                // 出栈消息：离线消息，未分发的publish消息
                Collection<Message> messages = sessionStore.getAllOfflineMsg(clientId);
                if (CollUtil.isNotEmpty(messages)) {
                    for (Message message : messages) {
                        if (!dispatcherMessage(clientId, message, publishMqttMsg)) {
                            return false;
                        }
                    }
                }
                sessionStore.clearOfflineMsg(clientId);
            }*/
            return true;
        }
    }

    class PutClient implements Runnable {
        @Override
        public void run() {
            while (!stoped) {
                if (clients.size() == 0) {
                    LockSupport.park(thread);
                }
                String clientId = clients.poll();
                ResendMessageTask resendMessageTask = new ResendMessageTask(clientId);
                long start = System.currentTimeMillis();
                try {
                    Boolean rs = sendMessageExecutor.submit(resendMessageTask).get(2000, TimeUnit.MILLISECONDS);
                    if (rs == null || !rs) {
                        log.warn("ReSend message is interrupted,the client offline again,clientId={}", clientId);
                    }
                    long cost = System.currentTimeMillis() - start;
                    log.debug("ReSend message clientId:{} cost time:{}", clientId, cost);
                } catch (Exception e) {
                    log.error("ReSend message failure,clientId:{}", clientId, e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                }
            }
            log.info("Shutdown re send message service success.");
        }
    }

}
