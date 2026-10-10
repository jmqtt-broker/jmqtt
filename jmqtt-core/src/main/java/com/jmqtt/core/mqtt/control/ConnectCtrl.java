package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.exception.ConnectException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.handler.ConnectHandler;
import com.jmqtt.core.mqtt.control.recover.ReSendMessageService;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelHelper;
import com.jmqtt.core.netty.session.ClientManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectCtrl extends BaseCtrl {

    private final ReSendMessageService reSendMessageService;

    public ConnectCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore,
                       MessageStore messageStore, SubscriptionMatcher subscriptionMatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
        this.reSendMessageService = new ReSendMessageService(sessionStore, messageStore);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttConnectMessage connectMessage = (MqttConnectMessage) mqttMessage;
        MqttConnectVariableHeader variableHeader = connectMessage.variableHeader();
        int mqttVersion = variableHeader.version();
        String clientId = connectMessage.payload().clientIdentifier();
        boolean cleanStart = variableHeader.isCleanSession();
        String userName = connectMessage.payload().userName();
        byte[] password = connectMessage.payload().passwordInBytes();
        boolean sessionPresent = false;
        String remoteAddress = ChannelHelper.getRemoteAddr(ctx.channel());
        try {
            ConnectHandler handler = clientHandlerMap.get((byte) mqttVersion);
            if (handler == null) {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, "authentication failed! protocol version not supported.");
            }
            handler.versionVerify(mqttVersion)
                    .clientIdVerify(clientId)
                    .blackListVerify(remoteAddress, clientId)
                    .authentication(clientId, userName, password, brokerConfig.getUser(), brokerConfig.getPassword(), brokerConfig.getAnonymous())
                    .keepAliveVerify(clientId, ctx, variableHeader.keepAliveTimeSeconds())
                    .verifySuccess(ctx, clientId, cleanStart, remoteAddress, connectMessage);
            reConnect2SendMessage(clientId);
        } catch (BrokerException e) {
            log.warn("connect failure, remoteAddress = {}, clientId = {}, returnCode = {}, reasonString = {}", remoteAddress, clientId, e.getReasonCode(), e.getReasonString());
            MqttConnAckMessage ackMessage = MessageUtil.getConnectAckMessage(e.getReasonCode(), sessionPresent, null);
            ctx.writeAndFlush(ackMessage).addListener(future -> {
                if (future.isDone()) {
                    ctx.close();
                }
            });
            ClientManager.removeClient(clientId);
        } catch (Exception e) {
            log.warn("connect failure, server error, remoteAddress = {}, clientId = {}", remoteAddress, clientId);
            MqttConnAckMessage ackMessage = MessageUtil.getConnectAckMessage(MqttConnectReturnCode.CONNECTION_REFUSED_UNSPECIFIED_ERROR, sessionPresent, null);
            ctx.writeAndFlush(ackMessage).addListener(future -> {
                if (future.isDone()) {
                    ctx.close();
                }
            });
            ClientManager.removeClient(clientId);
        }
    }

    private void reConnect2SendMessage(String clientId) {
        this.reSendMessageService.put(clientId);
        this.reSendMessageService.wakeUp();
    }

}
