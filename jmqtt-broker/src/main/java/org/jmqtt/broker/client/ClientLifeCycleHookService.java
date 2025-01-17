package org.jmqtt.broker.client;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.TimerManager;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.remoting.netty.ChannelEventListener;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Consumer;

public class ClientLifeCycleHookService implements ChannelEventListener {

    private static final Logger log = JmqttLogger.clientTraceLog;
    private SessionStore sessionStore;
    private MessageStore messageStore;
    private SubscriptionMatcher subscriptionMatcher;
    private InnerMessageDispatcher innerMessageDispatcher;

    public ClientLifeCycleHookService(SessionStore sessionStore,
                                      MessageStore messageStore,
                                      SubscriptionMatcher subscriptionMatcher,
                                      InnerMessageDispatcher innerMessageDispatcher) {
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.subscriptionMatcher = subscriptionMatcher;
        this.innerMessageDispatcher = innerMessageDispatcher;
    }

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        log.info("remoteAddr:[{}],Channel:{{}}", remoteAddr, channel.toString());
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        String clientId = NettyUtil.getClientId(channel);
        if (StringUtils.isNotEmpty(clientId)) {
            ClientSession session = ConnectManager.getInstance().getClient(clientId);
            if (session.isCleanStart()) {
                sessionStore.clearSession(clientId, false);
            } else {
                offlineSession(session);
                TimerManager.startSessionTimeout(clientId, (k, v) -> {
                    log.info("session expired. clientId: {}", clientId);
                    sessionStore.clearSession(clientId, false);
                });
            }
            // 收到DISCONNECT报文而断开的连接属于正常断开，不发送遗嘱消息，仅异常断开的连接发送遗嘱消息
            Boolean normalDisconnection = (Boolean) Optional.ofNullable(channel.attr(
                    AttributeKey.valueOf("NORMAL_DISCONNECTION")).get()).orElse(false);
            if (normalDisconnection) {
                if ((Boolean) Optional.ofNullable(channel.attr(
                        AttributeKey.valueOf("PUBLISH_WILL")).get()).orElse(false)) {
                    // 正常断开连接，但是收到的DISCONNECT中ReasonCode为0x04，表示即使是正常断开也需要发布遗嘱
                    publishWill(session);
                    messageStore.clearWillMessage(clientId);
                } else {
                    messageStore.clearWillAndWillRetain(clientId);
                }
            } else {
                // 异常断开，发布遗嘱
                publishWill(session);
            }
            ConnectManager.getInstance().removeClient(clientId);
            if (session.isMqtt5()) {
                TopicAliasManager.clear(clientId);
                sessionStore.clearClientProperty(clientId);
            }
        }
    }

    private void publishWill(ClientSession session) {
        String clientId = session.getClientId();
        Message willMessage = messageStore.getWillMessage(clientId);
        if (willMessage != null) {
            Consumer<Message> consumer = message -> {
                log.info("will message published, clientId: {}", clientId);
                innerMessageDispatcher.appendMessage(message);
                Optional.ofNullable(message.getHeader(MessageHeader.RETAIN)).ifPresent(retain -> {
                    if ((boolean) retain) {
                        log.info("will message store as retain, clientId: {}", clientId);
                        messageStore.storeRetainMessage((String) message.getHeader(MessageHeader.TOPIC), message);
                    }
                });
            };
            if (session.isMqtt5()) {
                TimerManager.startWillTimeout(clientId, willMessage, (k, v) -> {
                    consumer.accept((Message) v);
                });
            } else {
                consumer.accept(willMessage);
            }
        }
    }

    private void offlineSession(ClientSession clientSession) {
        String clientId = clientSession.getClientId();
        SessionState sessionState = new SessionState(SessionState.StateEnum.OFFLINE, System.currentTimeMillis(), clientSession.getVersion());
        SessionState exist = BrokerContext.getSessionStore().getSession(clientId);
        if (exist != null) {
            sessionState.setPropertyMap(exist.getPropertyMap());
            sessionStore.storeSession(clientId, sessionState);
        }
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        String clientId = NettyUtil.getClientId(channel);
        log.info("onChannelIdle:[{}],ClientId:{{}}", remoteAddr, clientId);
        Optional.ofNullable(ConnectManager.getInstance().getClient(clientId)).ifPresent(session -> {
            Mqtt5Utils.sendDisconnectAndClose(session, (byte) 0x8D);
        });
    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        String clientId = NettyUtil.getClientId(channel);
        LogUtil.warn(log, "[ClientLifeCycleHook] -> {} channelException,close channel and remove ConnectCache!", clientId);
    }

}
