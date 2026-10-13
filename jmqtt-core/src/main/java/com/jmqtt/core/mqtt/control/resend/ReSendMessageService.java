package com.jmqtt.core.mqtt.control.resend;

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
import java.util.function.Function;

@Slf4j
public class ReSendMessageService {

    private final Thread thread;
    private boolean stoped = false;
    private final BlockingQueue<String> clients = new LinkedBlockingQueue<>();
    private final SessionStore sessionStore;
    private final MessageStore messageStore;

    private final ExecutorService sendMessageExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ReSendMessageService(SessionStore sessionStore, MessageStore messageStore) {
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.thread = new Thread(new PutClient());
    }

    public boolean put(String clientId) {
        if (this.clients.size() > 10000) {
            log.warn("ReSend message busy! the client queue size is over {}", 10000);
            return false;
        }
        return this.clients.offer(clientId);
    }

    public void wakeUp() {
        LockSupport.unpark(thread);
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        if (!stoped) {
            stoped = true;
        }
    }

    public boolean dispatcherMessage(String clientId, Message message, Function<Message, MqttMessage> func) {
        ClientSession clientSession = ClientManager.getClient(clientId);
        if (clientSession == null) {
            log.warn("offline again: {}", clientId);
            return false;
        }
        MqttMessage mqttMessage = func.apply(message);
        return clientSession.getCtx().writeAndFlush(mqttMessage).isSuccess();
    }

    class ResendMessageTask implements Callable<Boolean> {

        private final String clientId;

        public ResendMessageTask(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public Boolean call() {

            // 入栈报文：未处理的pubRec报文
            Collection<Message> waitPubRecMsgs = sessionStore.getAllInflowMsg(clientId);
            if (CollUtil.isNotEmpty(waitPubRecMsgs)) {
                for (Message waitPubRecMsg : waitPubRecMsgs) {
                    if (!dispatcherMessage(clientId, waitPubRecMsg, message -> MessageUtil.getPubReplyMessage(message.getMsgId(), MqttMessageType.PUBREC, (byte) 0x00, null, true))) {
                        log.warn("ReSendMessageService resend inflow error,{}", waitPubRecMsg);
                    }
                }
            }

            // 出栈报文-qos1,qos2第一阶段：
            Collection<Message> outflowMsgs = sessionStore.getAllOutflowMsg(clientId);
            if (CollUtil.isNotEmpty(outflowMsgs)) {
                for (Message message : outflowMsgs) {
                    if (!dispatcherMessage(clientId, message, msg -> MessageUtil.getPubMessage(msg, false))) {
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
                    if (!dispatcherMessage(clientId, temp, message ->
                            MessageUtil.getPubReplyMessage(msgId, MqttMessageType.PUBREL, (byte) 0x00, null, true))) {
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
                if (clients.isEmpty()) {
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
                        log.error("resend interrupted", e1);
                    }
                }
            }
        }
    }

}
