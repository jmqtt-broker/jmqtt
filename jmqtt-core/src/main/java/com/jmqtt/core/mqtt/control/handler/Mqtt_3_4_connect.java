package com.jmqtt.core.mqtt.control.handler;

import cn.hutool.core.util.StrUtil;
import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.cluster.JmqttCluster;
import com.jmqtt.core.exception.ConnectException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;
import com.jmqtt.core.mqtt.model.Subscription;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionState;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.netty.session.ClientManager;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class Mqtt_3_4_connect implements ConnectHandler {

    protected final JmqttAuth jmqttAuth;

    protected final BrokerConfig brokerConfig;

    protected final ClusterConfig clusterConfig;

    protected final SessionStore sessionStore;

    protected final MessageStore messageStore;

    protected final SubscriptionMatcher subscriptionMatcher;

    public Mqtt_3_4_connect(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig, SessionStore sessionStore,
                            MessageStore messageStore, SubscriptionMatcher subscriptionMatcher) {
        this.jmqttAuth = jmqttAuth;
        this.brokerConfig = brokerConfig;
        this.clusterConfig = clusterConfig;
        this.sessionStore = sessionStore;
        this.messageStore = messageStore;
        this.subscriptionMatcher = subscriptionMatcher;
    }

    public ConnectHandler versionVerify(int mqttVersion) {
        if (mqttVersion != 3 && mqttVersion != 4 && mqttVersion != 5 || !this.jmqttAuth.versionVerify(mqttVersion)) {
            throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, "authentication failed! protocol version not supported.");
        }
        return this;
    }

    public ConnectHandler clientIdVerify(String clientId) {
        if (StrUtil.isBlank(clientId) || !this.jmqttAuth.clientIdVerify(clientId)) {
            throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, "authentication failed! clientId not allowed.");
        }
        return this;
    }

    public ConnectHandler blackListVerify(String remoteAddr, String clientId) {
        if (!this.jmqttAuth.blacklistVerify(remoteAddr, clientId)) {
            throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_BANNED, "authentication failed! client in blacklist.");
        }
        return this;
    }

    public ConnectHandler authentication(String clientId, String username, byte[] password, String defaultUser, String defaultPwd, boolean anonymousEnable) {
        if (!this.jmqttAuth.authentication(clientId, username, password, defaultUser, defaultPwd, anonymousEnable)) {
            throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, "authentication failed! bad username or password.");
        }
        return this;
    }

    public ConnectHandler keepAliveVerify(String clientId, ChannelHandlerContext ctx, int heatbeatSec) {
        if (this.jmqttAuth.heartbeatTimeVerify(clientId, heatbeatSec)) {
            int keepAlive = (int) (heatbeatSec * 1.5f);
            if (ctx.pipeline().names().contains("idleStateHandler")) {
                ctx.pipeline().remove("idleStateHandler");
            }
            ctx.pipeline().addFirst("idleStateHandler", new IdleStateHandler(keepAlive, 0, keepAlive));
        } else {
            throw ConnectException.of(MqttConnectReturnCode.CONNECTION_REFUSED_PROTOCOL_ERROR, StrUtil.format("authentication failed! keepalive time [{}] not allowed.", heatbeatSec));
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
        // 检测是否存在会话
        boolean sessionPresent = handlePreviousClient(ctx, clientId, cleanStart, version);
        // 处理遗嘱消息
        handleWillMessage(clientId, variableHeader, connectMessage);
        // 保存会话
        int heartbeatSec = variableHeader.keepAliveTimeSeconds();
        if (brokerConfig.isUseServerKeepalive() || (heartbeatSec <= 0 || heartbeatSec > 600)) {
            heartbeatSec = brokerConfig.getDefaultKeepalive();
        }
        SessionState ss = SessionState.builder()
                .brokerId(clusterConfig.getCurrentNodeName())
                .clientId(clientId)
                .version(version)
                .state(SessionState.StateEnum.ONLINE)
                .address(remoteAddress)
                .onlineTime(System.currentTimeMillis())
                .cleanStart(cleanStart)
                .keepalive(heartbeatSec)
                .build();
        sessionStore.storeSession(ss);
        connectAck(ctx, sessionPresent, null).addListener(future -> {
            if (future.isSuccess()) {
                log.info("[CONNECT success: {}] -> {} connect to this mqtt server", remoteAddress, clientId);
                // 同步会话到其他节点
                JmqttCluster.broadcast(ClusterMessage.of(ClusterMessage.EventType.SESSION_CONNECT, ss));
            }
        });
    }

    protected boolean handlePreviousClient(ChannelHandlerContext ctx, String clientId, boolean cleanStart, int version) {
        boolean present;
        // 检查当前节点查询是否存在该clientId，存在则踢下线
        Optional.ofNullable(ClientManager.getClient(clientId)).ifPresent(previous -> {
            MessageUtil.sendDisconnect(previous, Reason.SESSION_TAKEN_OVER);
            this.sessionStore.clearSession(clientId, true);
        });
        // 检查集群中其他节点是否存在该clientId，存在则踢下线
        SessionState state = sessionStore.getSession(clientId);
        ClientSession clientSession;
        if (SessionState.StateEnum.NEVER_CONNECTED.equals(state.getState())) {
            clientSession = new ClientSession(clientId, cleanStart, version, ctx);
            present = false;
            // notifyClearOtherSession = false;
        } else {
            if (cleanStart) {
                clientSession = newSession(clientId, version, ctx);
                present = false;
                // notifyClearOtherSession = false;
            } else {
                clientSession = reloadSession(ctx, clientId, version);
                present = true;
            }
        }
        ClientManager.putClient(clientId, clientSession);
        return present;
    }

    private ClientSession newSession(String clientId, int version, ChannelHandlerContext ctx) {
        ClientSession clientSession = new ClientSession(clientId, true, version, ctx);
        this.sessionStore.clearSession(clientId, true);
        return clientSession;
    }

    private ClientSession reloadSession(ChannelHandlerContext ctx, String clientId, int version) {
        ClientSession clientSession = new ClientSession(clientId, false, version, ctx);
        Set<Subscription> subscriptions = sessionStore.getSubscriptions(clientId);
        for (Subscription subscription : subscriptions) {
            this.subscriptionMatcher.subscribe(subscription);
            // ClusterHelper.subscribeToCluster(subscription);
        }
        return clientSession;
    }

    protected void handleWillMessage(String clientId, MqttConnectVariableHeader variableHeader, MqttConnectMessage connectMessage) {
        if (variableHeader.isWillFlag()) {
            MqttConnectPayload payload = connectMessage.payload();
            byte[] content = payload.willMessageInBytes();
            if (content.length > 0) {
                boolean willRetain = variableHeader.isWillRetain();
                int willQos = variableHeader.willQos();
                String willTopic = payload.willTopic();
                storeWillMsg(clientId, willRetain, willQos, willTopic, payload.willMessageInBytes(), null);
            } else {
                messageStore.clearWillAndWillRetain(clientId);
            }
        }
    }

    protected void storeWillMsg(String clientId, boolean willRetain, int willQos,
                                String willTopic, byte[] willPayload, Map<Integer, Object> propertyMap) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageHeader.RETAIN, willRetain);
        headers.put(MessageHeader.QOS, willQos);
        headers.put(MessageHeader.TOPIC, willTopic);
        headers.put(MessageHeader.WILL, true);
        headers.put(MessageHeader.REMAINING_LENGTH, willPayload.length);
        Message message = new Message(headers, willPayload);
        message.setProperties(propertyMap);
        message.setStoreTime(System.currentTimeMillis());
        message.setClientId(clientId);
        messageStore.storeWillMessage(clientId, message);
        // ClusterHelper.clearRetainMessage(willTopic);
        log.info("[WillMessageStore] : {} store will message:{}", clientId, message);
    }

    protected ChannelFuture connectAck(ChannelHandlerContext ctx, boolean sessionPresent, MqttProperties responseProperties) {
        MqttConnAckMessage ackMessage = MessageUtil.getConnectAckMessage(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent, responseProperties);
        return ctx.writeAndFlush(ackMessage);
    }

}
