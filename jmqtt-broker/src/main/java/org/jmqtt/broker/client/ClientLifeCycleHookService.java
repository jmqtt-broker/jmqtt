package org.jmqtt.broker.client;

import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.TimerUtils;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.processor.dispatcher.akka.ClusterHelper;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.remoting.netty.ChannelEventListener;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.slf4j.Logger;

import java.util.Optional;

public class ClientLifeCycleHookService implements ChannelEventListener {

    private static final Logger log = JmqttLogger.clientTraceLog;
    private SessionStore sessionStore;
    private MessageStore messageStore;
    private InnerMessageDispatcher innerMessageDispatcher;

    public ClientLifeCycleHookService(SessionStore sessionStore,
                                      MessageStore messageStore,
                                      InnerMessageDispatcher innerMessageDispatcher) {
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
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
            Optional.ofNullable(ConnectManager.getInstance().getClient(clientId)).ifPresent(session -> {
                if (session.isCleanStart()) {
                    sessionStore.clearSession(clientId, true);
                } else {
                    offlineSession(session);
                    TimerUtils.startSessionTimeout(clientId);
                }
                if (session.normalDisconnection()) {
                    // 收到DISCONNECT报文而断开的连接属于正常断开，不发送遗嘱消息，仅异常断开的连接发送遗嘱消息
                    if (session.publishWill()) {
                        // 正常断开连接，但是收到的DISCONNECT中ReasonCode为0x04，表示客户端希望即使是正常断开也需要发布遗嘱
                        publishWill(session);
                    } else {
                        messageStore.clearWillAndWillRetain(clientId);
                    }
                } else {
                    // 未收到DISCONNECT报文，异常断开，发布遗嘱消息
                    publishWill(session);
                }
                ConnectManager.getInstance().removeClient(clientId);
                if (session.isMqtt5()) {
                    TopicAliasManager.clear(clientId);
                    sessionStore.clearClientProperty(clientId);
                }
            });
        }
    }

    private void publishWill(ClientSession session) {
        String clientId = session.getClientId();
        Message willMessage = messageStore.getWillMessage(clientId);
        if (willMessage != null) {
            if (session.isMqtt5()) {
                TimerUtils.startWillTimeout(clientId, willMessage);
            } else {
                innerMessageDispatcher.appendMessage(willMessage);
                Optional.ofNullable(willMessage.getHeader(MessageHeader.RETAIN)).ifPresent(retain -> {
                    if ((boolean) retain) {
                        messageStore.storeRetainMessage((String) willMessage.getHeader(MessageHeader.TOPIC), willMessage);
                    }
                });
                messageStore.clearWillMessage(clientId);
            }
        }
    }

    private void offlineSession(ClientSession clientSession) {
        String clientId = clientSession.getClientId();
        SessionState sessionState = new SessionState(SessionState.StateEnum.OFFLINE, System.currentTimeMillis(), clientSession.getVersion());
        SessionState exist = BrokerContext.getSessionStore().getSession(clientId);
        if (exist != null) {
            sessionState.setClientId(clientId);
            sessionState.setPropertyMap(exist.getPropertyMap());
            sessionState.setAddress(exist.getAddress());
            sessionState.setOnlineTime(exist.getOnlineTime());
            sessionState.setCleanStart(exist.getCleanStart());
            sessionState.setKeepalive(exist.getKeepalive());
            if (ClusterHelper.lightning()) {
                ClusterHelper.reportSessionToKeeper(sessionState);
            }
            sessionStore.storeSession(sessionState);
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
