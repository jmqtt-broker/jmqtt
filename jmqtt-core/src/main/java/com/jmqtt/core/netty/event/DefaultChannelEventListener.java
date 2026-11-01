package com.jmqtt.core.netty.event;

import cn.hutool.core.util.StrUtil;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.helper.TopicAliasManager;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class DefaultChannelEventListener implements ChannelEventListener {

    private final SessionStore sessionStore;

    private final MessageStore messageStore;

    private final MessageDispatcher messageDispatcher;

    public DefaultChannelEventListener(SessionStore sessionStore, MessageStore messageStore, MessageDispatcher messageDispatcher) {
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
        log.info("onChannelConnect, remoteAddr:[{}],Channel:{{}}", remoteAddr, channel.toString());
    }

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        log.info("onChannelClose, remoteAddr:[{}],Channel:{{}}", remoteAddr, channel.toString());
        String clientId = ChannelUtil.getClientId(channel);
        if (StrUtil.isNotEmpty(clientId)) {
            ClientSession session = ClientManager.getClient(clientId);
            if (session != null) {
                // offlineSession(session);
                if (session.normalDisconnection()) {
                    // 收到DISCONNECT报文而断开的连接属于正常断开，不发送遗嘱消息，仅异常断开的连接发送遗嘱消息
                    if (session.publishWill()) {
                        // 正常断开连接，但是收到的DISCONNECT中ReasonCode为0x04，表示客户端希望即使是正常断开也需要发布遗嘱
                        // publishWill(session);
                    } else {
                        messageStore.clearWillAndWillRetain(clientId);
                    }
                } else {
                    // 未收到DISCONNECT报文，异常断开，发布遗嘱消息
                    publishWill(session);
                }
                ClientManager.removeClient(clientId);
                if (session.isMqtt5()) {
                    TopicAliasManager.clear(clientId);
                }
            }
        }
    }

    private void publishWill(ClientSession session) {
        String clientId = session.getClientId();
        Message willMessage = messageStore.getWillMessage(clientId);
        if (willMessage != null) {
            if (session.isMqtt5()) {
                // TimerUtils.startWillTimeout(clientId, willMessage);
            } else {
                messageDispatcher.accept(willMessage);
                Optional.ofNullable(willMessage.getHeader(MessageHeader.RETAIN)).ifPresent(retain -> {
                    if ((boolean) retain) {
                        messageStore.retainHandle(willMessage);
                    }
                });
                messageStore.clearWillMessage(clientId);
            }
        }
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        String clientId = ChannelUtil.getClientId(channel);
        log.debug("onChannelIdle, remoteAddr:[{}],ClientId:{{}}", remoteAddr, clientId);

    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        String clientId = ChannelUtil.getClientId(channel);
        log.warn("onChannelException, remoteAddr:[{}],ClientId:{{}}", remoteAddr, clientId);

    }

}
