package com.jmqtt.core.mqtt.processor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.exception.ConnectException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionState;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.netty.helper.ChannelHelper;
import com.jmqtt.core.netty.helper.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.*;

@Slf4j
public class ConnectProcessor implements RequestProcessor {

    private final JmqttAuth jmqttAuth;

    private final BrokerConfig brokerConfig;

    private final SessionStore sessionStore;

    private final MessageStore messageStore;

    private final Map<Byte, ConnectHandler> clientHandlerMap;

    public ConnectProcessor(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, SessionStore sessionStore, MessageStore messageStore) {
        this.jmqttAuth = jmqttAuth;
        this.brokerConfig = brokerConfig;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        Mqtt_3_4 mqtt3_4 = new Mqtt_3_4(jmqttAuth, brokerConfig, sessionStore, messageStore);
        Mqtt_5_0 mqtt5 = new Mqtt_5_0(jmqttAuth, brokerConfig, sessionStore, messageStore);
        this.clientHandlerMap = Map.of(
                MqttVersion.MQTT_3_1.protocolLevel(), mqtt3_4,
                MqttVersion.MQTT_3_1_1.protocolLevel(), mqtt3_4,
                MqttVersion.MQTT_5.protocolLevel(), mqtt5
        );
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
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
            handler.versionVerify(mqttVersion);
            handler.clientIdVerify(clientId);
            handler.blackListVerify(remoteAddress, clientId);
            handler.authentication(clientId, userName, password, brokerConfig.getUser(), brokerConfig.getPassword(), brokerConfig.getAnonymous());
            handler.keepAlive(clientId, ctx, variableHeader.keepAliveTimeSeconds());
            handler.verifySuccess(ctx.channel(), clientId, cleanStart, connectMessage);
        } catch (BrokerException e) {
            log.warn("connect failure, remoteAddress = {}, clientId = {}, returnCode = {}, reasonString = {}", remoteAddress, clientId, e.getReasonCode(), e.getReasonString());
            MqttConnAckMessage ackMessage = MessageUtil.getConnectAckMessage(e.getReasonCode(), sessionPresent, null);
            ctx.writeAndFlush(ackMessage).addListener(future -> {
                if (future.isDone()) {
                    ctx.close();
                }
            });
        } catch (Exception e) {
            log.warn("connect failure, server error, remoteAddress = {}, clientId = {}", remoteAddress, clientId);
            MqttConnAckMessage ackMessage = MessageUtil.getConnectAckMessage(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, sessionPresent, null);
            ctx.writeAndFlush(ackMessage).addListener(future -> {
                if (future.isDone()) {
                    ctx.close();
                }
            });
        }

    }

    interface ConnectHandler {
        void versionVerify(int mqttVersion);
        void clientIdVerify(String clientId);
        void blackListVerify(String remoteAddr, String clientId);
        void authentication(String clientId, String userName, byte[] password, String defaultUser, String defaultPwd, boolean anonymousEnable);
        void keepAlive(String clientId, ChannelHandlerContext ctx, int heatbeatSec);

        void verifySuccess(Channel channel, String clientId, boolean cleanStart, MqttConnectMessage connectMessage);
    }

    static class Mqtt_3_4 implements ConnectHandler {

        protected final JmqttAuth jmqttAuth;

        protected final BrokerConfig brokerConfig;

        protected final SessionStore sessionStore;

        protected final MessageStore messageStore;

        public Mqtt_3_4(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, SessionStore sessionStore, MessageStore messageStore) {
            this.jmqttAuth = jmqttAuth;
            this.brokerConfig = brokerConfig;
            this.sessionStore = sessionStore;
            this.messageStore = messageStore;
        }

        public void versionVerify(int mqttVersion) {
            if (mqttVersion != 3 && mqttVersion != 4 && mqttVersion != 5 || !this.jmqttAuth.versionVerify(mqttVersion)) {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, "authentication failed! protocol version not supported.");
            }
        }

        public void clientIdVerify(String clientId) {
            if (StrUtil.isBlank(clientId) || !this.jmqttAuth.clientIdVerify(clientId)) {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, "authentication failed! clientId not allowed.");
            }
        }

        public void blackListVerify(String remoteAddr, String clientId) {
            if (!this.jmqttAuth.blacklistVerify(remoteAddr, clientId)) {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_BANNED, "authentication failed! client in blacklist.");
            }
        }

        public void authentication(String clientId, String username, byte[] password, String defaultUser, String defaultPwd, boolean anonymousEnable) {
            if (!this.jmqttAuth.authentication(clientId, username, password, defaultUser, defaultPwd, anonymousEnable)) {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, "authentication failed! bad username or password.");
            }
        }

        public void keepAlive(String clientId, ChannelHandlerContext ctx, int heatbeatSec) {
            if (this.jmqttAuth.heartbeatTimeVerify(clientId, heatbeatSec)) {
                int keepAlive = (int) (heatbeatSec * 1.5f);
                if (ctx.pipeline().names().contains("idleStateHandler")) {
                    ctx.pipeline().remove("idleStateHandler");
                }
                ctx.pipeline().addFirst("idleStateHandler", new IdleStateHandler(keepAlive, 0, keepAlive));
            } else {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_PROTOCOL_ERROR, "authentication failed! keepalive time not allowed.");
            }
        }

        public void verifySuccess(Channel channel, String clientId, boolean cleanStart, MqttConnectMessage connectMessage) {
            // 从集群/本服务器中查询是否存在该clientId的设备
            SessionState sessionState = sessionStore.getSession(clientId);

            NettyUtil.setClientId(channel, clientId);
        }

        protected void handlePreviousClient() {

        }
    }

    static class Mqtt_5_0 extends Mqtt_3_4 {

        public Mqtt_5_0(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, SessionStore sessionStore, MessageStore messageStore) {
            super(jmqttAuth, brokerConfig, sessionStore, messageStore);
        }

        public void clientIdVerify(String clientId) {
            if (!this.jmqttAuth.clientIdVerify(clientId)) {
                throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, "authentication failed! clientId not allowed.");
            }
        }

        public void verifySuccess(Channel channel, String clientId, boolean cleanStart, MqttConnectMessage connectMessage) {
            MqttProperties responseProperties = new MqttProperties();
            if (clientId.isEmpty()) {
                // 客户端未设置clientId，返回服务端生成的clientId给客户端
                clientId = brokerConfig.getClientIdPrefix() + IdUtil.getSnowflakeNextIdStr();
                responseProperties.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.ASSIGNED_CLIENT_IDENTIFIER.value(), clientId));
            }
            // 设置心跳，mqtt5中可以使用服务端设置的默认心跳时间
            MqttConnectVariableHeader variableHeader = connectMessage.variableHeader();
            int heartbeatSec = variableHeader.keepAliveTimeSeconds();
            if (brokerConfig.isUseServerKeepalive() || (heartbeatSec <= 0 || heartbeatSec > 600)) {
                heartbeatSec = brokerConfig.getDefaultKeepalive();
                responseProperties.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.SERVER_KEEP_ALIVE.value(), heartbeatSec));
            }
            optionalService(responseProperties);

        }

        public void optionalService(MqttProperties properties) {
            // 服务端能同时处理的非qos0最大消息数，暂定int最大值，后面放到配置里
            properties.add(new MqttProperties.IntegerProperty(
                    MqttProperties.MqttPropertyType.RECEIVE_MAXIMUM.value(),
                    brokerConfig.getReceiveMaximum()));
            // 服务端能处理的最大packet长度，默认20M，后面放到配置里
            properties.add(new MqttProperties.IntegerProperty(
                    MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE.value(),
                    brokerConfig.getMaximumPacketSize()));
            // 主题别名最大值
            properties.add(new MqttProperties.IntegerProperty(
                    MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM.value(),
                    brokerConfig.getTopicAliasMaximum()));
            // 可选功能
            properties.add(new MqttProperties.IntegerProperty(WILDCARD_SUBSCRIPTION_AVAILABLE.value(), brokerConfig.getWildcardSubscriptionAvailable() ? 1 : 0));
            properties.add(new MqttProperties.IntegerProperty(SUBSCRIPTION_IDENTIFIER_AVAILABLE.value(), brokerConfig.getSubscriptionIdentifierAvailable() ? 1 : 0));
            properties.add(new MqttProperties.IntegerProperty(SHARED_SUBSCRIPTION_AVAILABLE.value(), brokerConfig.getSharedSubscriptionAvailable() ? 1 : 0));
            properties.add(new MqttProperties.IntegerProperty(RETAIN_AVAILABLE.value(), brokerConfig.getRetainAvailable() ? 1 : 0));
            properties.add(new MqttProperties.IntegerProperty(MAXIMUM_QOS.value(), brokerConfig.getMaximumQos()));
        }
    }

}
