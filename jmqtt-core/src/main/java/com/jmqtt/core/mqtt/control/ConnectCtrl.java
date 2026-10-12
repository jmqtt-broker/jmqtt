package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.exception.ConnectException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.handler.ConnectHandler;
import com.jmqtt.core.mqtt.control.handler.Mqtt_3_4_connect;
import com.jmqtt.core.mqtt.control.handler.Mqtt_5_connect;
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

import java.util.Map;

@Slf4j
public class ConnectCtrl extends BaseCtrl {

    protected final Map<Byte, ConnectHandler> connectHandlerMap;

    private final ReSendMessageService reSendMessageService;

    public ConnectCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore,
                       MessageStore messageStore, SubscriptionMatcher subscriptionMatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
        Mqtt_3_4_connect mqtt_3_4_connect = new Mqtt_3_4_connect(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
        Mqtt_5_connect mqtt_5_connect = new Mqtt_5_connect(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
        this.connectHandlerMap = Map.of(
                MqttVersion.MQTT_3_1.protocolLevel(), mqtt_3_4_connect,
                MqttVersion.MQTT_3_1_1.protocolLevel(), mqtt_3_4_connect,
                MqttVersion.MQTT_5.protocolLevel(), mqtt_5_connect
        );
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
            ConnectHandler handler = connectHandlerMap.get((byte) mqttVersion);
            if (handler == null) {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, "authentication failed! protocol version not supported.");
            }
            handler.versionVerify(mqttVersion)
                    .clientIdVerify(clientId)
                    .blackListVerify(remoteAddress, clientId)
                    .authentication(clientId, userName, password, brokerConfig.getUser(), brokerConfig.getPassword(), brokerConfig.getAnonymous())
                    .keepAliveVerify(clientId, ctx, variableHeader.keepAliveTimeSeconds())
                    .verifySuccess(ctx, clientId, cleanStart, remoteAddress, connectMessage);
            // 重新发送未确认的消息报文
            reConnect2SendMessage(clientId);
        } catch (ConnectException e) {
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
