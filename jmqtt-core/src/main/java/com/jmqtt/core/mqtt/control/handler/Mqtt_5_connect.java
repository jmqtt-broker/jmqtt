package com.jmqtt.core.mqtt.control.handler;

import cn.hutool.core.util.IdUtil;
import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.exception.ConnectException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionState;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.*;

@Slf4j
public class Mqtt_5_connect extends Mqtt_3_4_connect {

    public Mqtt_5_connect(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore,
                          MessageStore messageStore, SubscriptionMatcher subscriptionMatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher);
    }

    public ConnectHandler clientIdVerify(String clientId) {
        if (!this.jmqttAuth.clientIdVerify(clientId)) {
            throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, "authentication failed! clientId not allowed.");
        }
        return this;
    }

    public void verifySuccess(ChannelHandlerContext ctx, String clientId, boolean cleanStart,
                              String remoteAddress, MqttConnectMessage connectMessage) {
        MqttConnectVariableHeader variableHeader = connectMessage.variableHeader();
        int version = variableHeader.version();
        // 给当前连接设置clientId和version
        ChannelUtil.setClientId(ctx.channel(), clientId);
        ChannelUtil.setVersion(ctx.channel(), version);
        MqttProperties responseProperties = new MqttProperties();
        if (clientId.isEmpty()) {
            // 客户端未设置clientId，返回服务端生成的clientId给客户端
            clientId = brokerConfig.getClientIdPrefix() + IdUtil.getSnowflakeNextIdStr();
            responseProperties.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.ASSIGNED_CLIENT_IDENTIFIER.value(), clientId));
        }
        // 设置心跳，mqtt5中可以使用服务端设置的默认心跳时间
        int heartbeatSec = variableHeader.keepAliveTimeSeconds();
        if (brokerConfig.isUseServerKeepalive() || (heartbeatSec <= 0 || heartbeatSec > 600)) {
            heartbeatSec = brokerConfig.getDefaultKeepalive();
            responseProperties.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.SERVER_KEEP_ALIVE.value(), heartbeatSec));
        }
        // mqtt5连接选项
        optionalService(responseProperties);
        // 检测是否存在会话
        boolean sessionPresent = handlePreviousClient(ctx, clientId, cleanStart, version);
        // 如果存在会话和遗嘱消息的超时任务，则停止
        stopSessionAndWillMessageTimeout(clientId);
        // 处理遗嘱消息
        handleWillMessage(clientId, variableHeader, connectMessage);
        // 保存会话
        SessionState ss = SessionState.builder()
                .brokerId(clusterConfig.getCurrentNodeName())
                .clientId(clientId)
                .version(version)
                .state(SessionState.StateEnum.ONLINE)
                .address(remoteAddress)
                .onlineTime(System.currentTimeMillis())
                .cleanStart(cleanStart)
                .keepalive(heartbeatSec)
                .propertyMap(MessageUtil.propertyMap(variableHeader.properties()))
                .build();
        sessionStore.storeSession(ss);
        connectAck(ctx, sessionPresent, responseProperties);
        log.info("[CONNECT success: {}] -> {} connect to this mqtt server", remoteAddress, clientId);
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

    private void stopSessionAndWillMessageTimeout(String clientId) {
        // TimerUtils.stopSessionTimeout(clientId);
        // TimerUtils.stopWillTimeout(clientId);
    }

    protected void handleWillMessage(String clientId, MqttConnectVariableHeader variableHeader, MqttConnectMessage connectMessage) {
        if (variableHeader.isWillFlag()) {
            MqttConnectPayload payload = connectMessage.payload();
            byte[] content = payload.willMessageInBytes();
            if (content.length > 0) {
                boolean willRetain = variableHeader.isWillRetain();
                int willQos = variableHeader.willQos();
                String willTopic = payload.willTopic();
                if (!brokerConfig.getRetainAvailable() && willRetain) {
                    log.warn("retain not available, clientId: {}", clientId);
                    throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED, "retain not available.");
                }
                if (willQos > brokerConfig.getMaximumQos()) {
                    log.warn("QoS not supported, clientId: {}", clientId);
                    throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_QOS_NOT_SUPPORTED, "QoS not supported.");
                }
                if (checkPackageSize(clientId, connectMessage.fixedHeader().remainingLength())) {
                    log.warn("exceeding packet size, clientId: {}", clientId);
                    throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_PACKET_TOO_LARGE, "exceeding packet size.");
                }
                MqttProperties properties = payload.willProperties();
                storeWillMsg(clientId, willRetain, willQos, willTopic, payload.willMessageInBytes(), MessageUtil.propertyMap(properties));
            } else {
                messageStore.clearWillAndWillRetain(clientId);
            }
        }
    }

    private boolean checkPackageSize(String clientId, int remainingLength) {
        boolean res = false;
        // 固定头长度2~5字节，这里直接以5为准
        int packetSize = getMsgSize(remainingLength);
        Integer maxSize = (Integer) sessionStore.getClientProperty(clientId, MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE.value());
        if (maxSize != null) {
            if (packetSize > maxSize) {
                // 当包大小大于连接时约定的值时，服务端主动发送DISCONNECT
                log.warn("client max packet size error,clientId {}", clientId);
                res = true;
            }
        }
        if (packetSize > brokerConfig.getMaximumPacketSize()) {
            // 当包大小大于服务端允许接收的最大长度时
            log.warn("server max packet size error,clientId {}", clientId);
            res = true;
        }
        return res;
    }

    /**
     * 固定报头存在于所有的 MQTT 控制报文中，其长度是 2 到 5 个字节。它由两部分组成：
     * 1. 报文类型与标志位（1 字节）： 第 1 个字节固定占用 1 个字节，高 4 位表示报文类型（如 CONNECT、PUBLISH 等），
     * 低 4 位是标志位（如 QoS、DUP、RETAIN 等）。
     * 2. 剩余长度字段（1 到 4 字节）： 从第 2 个字节开始，采用一种“可变字节整数”的编码方式来表示“可变报头 + 有效载荷”的总字节数。
     * 如果剩余长度在 0~127 字节之间，占用 1 个字节。
     * 如果剩余长度在 128~16383 字节之间，占用 2 个字节。
     * 如果剩余长度在 16384~2097151 字节之间，占用 3 个字节。
     * 如果剩余长度在 2097152~268435455 字节之间，占用 4 个字节。
     *
     * @param remainingLength
     * @return
     */
    private int getMsgSize(int remainingLength) {
        if (remainingLength <= 127) {
            return 1 + 1 + remainingLength;
        } else if (remainingLength <= 16383) {
            return 1 + 2 + remainingLength;
        } else if (remainingLength <= 2097151) {
            return 1 + 3 + remainingLength;
        } else if (remainingLength <= 268435455) {
            return 1 + 4 + remainingLength;
        }
        return 1 + 4 + remainingLength;
    }

}
