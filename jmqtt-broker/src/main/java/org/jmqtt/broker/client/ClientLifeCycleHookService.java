package org.jmqtt.broker.client;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.TimerManager;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.remoting.netty.ChannelEventListener;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

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
            Boolean normalDisconnection = (Boolean) Optional.ofNullable(session.getCtx().channel()
                    .attr(AttributeKey.valueOf("NORMAL_DISCONNECTION")).get()).orElse(false);
            if (session.isCleanStart()) {
                clearSession(session);
            } else {
                offlineSession(session);
                TimerManager.startSessionTimeout(clientId, (k, v) -> clearSession(session));
            }
            ConnectManager.getInstance().removeClient(clientId);
            // 收到DISCONNECT报文而断开的连接属于正常断开，不发送遗嘱消息，仅异常断开的连接发送遗嘱消息
            if (!normalDisconnection) {
                Message willMessage = messageStore.getWillMessage(clientId);
                if (willMessage != null) {
                    if (session.isMqtt5()) {
                        TimerManager.startWillTimeout(clientId, willMessage, (k, v) -> {
                            innerMessageDispatcher.appendMessage((Message) v);
                            messageStore.clearWillMessage(clientId);
                        });
                    } else {
                        innerMessageDispatcher.appendMessage(willMessage);
                        messageStore.clearWillMessage(clientId);
                    }
                }
            }
        }
    }

    private void clearSession(ClientSession clientSession) {
        String clientId = clientSession.getClientId();
        Set<Subscription> subscriptions = sessionStore.getSubscriptions(clientId);
        for (Subscription subscription : subscriptions) {
            this.subscriptionMatcher.unSubscribe(subscription.getTopic(), clientId);
        }
        sessionStore.clearSession(clientId, false);
        if (clientSession.isMqtt5()) {
            TopicAliasManager.clear(clientId);
            sessionStore.clearClientProperty(clientId);
            // 会话到期了，如果存在延迟未发送的遗嘱消息，此时需要立即发送
            TimerManager.sendWillImmediately(clientId);
        }
    }

    private void offlineSession(ClientSession clientSession) {
        String clientId = clientSession.getClientId();
        SessionState sessionState = new SessionState(SessionState.StateEnum.OFFLINE, System.currentTimeMillis());
        SessionState exist = BrokerContext.getSessionStore().getSession(clientId);
        if (exist != null) {
            sessionState.setPropertyMap(exist.getPropertyMap());
            sessionStore.storeSession(clientId, sessionState);
        }
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        String clientId = NettyUtil.getClientId(channel);
        ConnectManager.getInstance().removeClient(clientId);
        LogUtil.warn(log, "[ClientLifeCycleHook] -> {} channelException,close channel and remove ConnectCache!", clientId);
    }

}
