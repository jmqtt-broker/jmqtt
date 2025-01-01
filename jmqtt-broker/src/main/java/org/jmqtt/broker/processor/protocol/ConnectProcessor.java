package org.jmqtt.broker.processor.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.helper.TimerManager;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.event.Event;
import org.jmqtt.broker.processor.dispatcher.event.EventCode;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.processor.recover.ReSendMessageService;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.IdWorker;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.remoting.util.RemotingHelper;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * mqtt 客户端连接逻辑处理 强制约束：jmqtt在未返回conAck之前，不接收其它任何mqtt协议报文（mqtt协议可以允许） TODO mqtt5 协议支持
 */
public class ConnectProcessor implements RequestProcessor {

    private static final Logger log = JmqttLogger.clientTraceLog;

    private AuthValid authValid;
    private ReSendMessageService reSendMessageService;
    private SubscriptionMatcher subscriptionMatcher;
    private SessionStore sessionStore;
    private MessageStore messageStore;
    private ClusterEventHandler clusterEventHandler;

    private String user;
    private String pwd;
    private boolean anonymousEnable = false;
    private BrokerConfig brokerConfig;

    public ConnectProcessor(BrokerController brokerController) {
        this.authValid = brokerController.getAuthValid();
        this.reSendMessageService = brokerController.getReSendMessageService();
        this.subscriptionMatcher = brokerController.getSubscriptionMatcher();
        this.sessionStore = brokerController.getSessionStore();
        this.messageStore = brokerController.getMessageStore();
        this.clusterEventHandler = brokerController.getClusterEventHandler();
        this.user = brokerController.getBrokerConfig().getUser();
        this.pwd = brokerController.getBrokerConfig().getPwd();
        this.anonymousEnable = brokerController.getBrokerConfig().isAnonymousEnable();
        this.brokerConfig = brokerController.getBrokerConfig();
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttConnectMessage connectMessage = (MqttConnectMessage) mqttMessage;
        MqttConnectReturnCode returnCode;
        MqttConnectVariableHeader variableHeader = connectMessage.variableHeader();
        int mqttVersion = variableHeader.version();
        boolean mqtt5 = mqttVersion == MqttVersion.MQTT_5.protocolLevel();
        String clientId = connectMessage.payload().clientIdentifier();
        boolean cleanSession = variableHeader.isCleanSession();
        String userName = connectMessage.payload().userName();
        byte[] password = connectMessage.payload().passwordInBytes();
        ClientSession clientSession = null;
        boolean sessionPresent = false;
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        MqttProperties responseProperties = new MqttProperties();
        try {
            if (!versionValid(mqttVersion)) {
                returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
            } else if (!clientIdVerify(clientId)) {
                returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
            } else if (onBlackList(RemotingHelper.getRemoteAddr(ctx.channel()), clientId)) {
                returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
            } else if (!authentication(clientId, userName, password, this.user, this.pwd, this.anonymousEnable)) {
                returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
            } else {
                if (mqtt5 && clientId.isEmpty()) {
                    // 客户端未设置clientId，返回服务端生成的clientId给客户端
                    clientId = brokerConfig.getClientIdPrefix() + IdWorker.getId();
                    responseProperties.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.ASSIGNED_CLIENT_IDENTIFIER.value(), clientId));
                }
                // 1. 设置心跳
                int heartbeatSec = variableHeader.keepAliveTimeSeconds();
                if (mqtt5 && (brokerConfig.isUseServerKeepalive() || (heartbeatSec <= 0 || heartbeatSec > 600))) {
                    heartbeatSec = brokerConfig.getDefaultKeepalive();
                    responseProperties.add(new MqttProperties.IntegerProperty(MqttProperties.MqttPropertyType.SERVER_KEEP_ALIVE.value(), heartbeatSec));
                }
                if (!keepAlive(clientId, ctx, heartbeatSec)) {
                    LogUtil.warn(log, "[CONNECT] -> set heartbeat failure,clientId:{},heartbeatSec:{}", clientId, heartbeatSec);
                    throw new Exception("set heartbeat failure");
                }
                // 2. 从集群/本服务器中查询是否存在该clientId的设备消息
                SessionState sessionState = sessionStore.getSession(clientId);
                boolean notifyClearOtherSession = true;
                if (sessionState.getState() == SessionState.StateEnum.ONLINE) {
                    ClientSession previousClient = ConnectManager.getInstance().getClient(clientId);
                    if (previousClient != null) {
                        previousClient.getCtx().close();
                        ConnectManager.getInstance().removeClient(clientId);
                        notifyClearOtherSession = false;
                    }
                }
                if (sessionState.getState() == SessionState.StateEnum.NULL) {
                    clientSession = new ClientSession(clientId, cleanSession, mqttVersion, ctx);
                    sessionPresent = false;
                    notifyClearOtherSession = false;
                } else {
                    if (cleanSession) {
                        clientSession = createNewClientSession(clientId, mqttVersion, ctx);
                        sessionPresent = false;
                        notifyClearOtherSession = false;
                    } else {
                        clientSession = reloadClientSession(ctx, clientId, mqttVersion);
                        sessionPresent = true;
                    }
                    if (clientSession.isMqtt5()) {
                        TimerManager.stopSessionTimeout(clientId);
                        TimerManager.stopWillTimeout(clientId);
                    }
                }
                SessionState ss = new SessionState(SessionState.StateEnum.ONLINE);
                if (mqtt5) {
                    // 服务端能同时处理的非qos0最大消息数，暂定int最大值，后面放到配置里
                    responseProperties.add(new MqttProperties.IntegerProperty(
                            MqttProperties.MqttPropertyType.RECEIVE_MAXIMUM.value(),
                            brokerConfig.getReceiveMaximum()));
                    // 服务端能处理的最大packet长度，默认20M，后面放到配置里
                    responseProperties.add(new MqttProperties.IntegerProperty(
                            MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE.value(),
                            brokerConfig.getMaximumPacketSize()));
                    // 主题别名最大值
                    responseProperties.add(new MqttProperties.IntegerProperty(
                            MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM.value(),
                            brokerConfig.getTopicAliasMaximum()));
                    Map<Integer, Object> propertyMap = Mqtt5Utils.propertyMap(variableHeader.properties());
                    if (!propertyMap.isEmpty()) {
                        ss.setPropertyMap(propertyMap);
                        clientSession.setPropertyMap(propertyMap);
                    }
                }
                // 3. 存储 session 会话
                sessionStore.storeSession(clientId, ss);
                if (notifyClearOtherSession) {
                    Event event = new Event(EventCode.CLEAR_SESSION.getCode(), clientId, System.currentTimeMillis(), MixAll.getLocalIp());
                    clusterEventHandler.sendEvent(event);
                }

                // 4. 处理will 消息
                boolean willFlag = variableHeader.isWillFlag();
                if (willFlag) {
                    boolean willRetain = variableHeader.isWillRetain();
                    int willQos = variableHeader.willQos();
                    MqttConnectPayload payload = connectMessage.payload();
                    String willTopic = payload.willTopic();
                    byte[] willPayload = payload.willMessageInBytes();
                    MqttProperties properties = payload.willProperties();
                    storeWillMsg(clientId, willRetain, willQos, willTopic, willPayload, Mqtt5Utils.propertyMap(properties));
                }
                returnCode = MqttConnectReturnCode.CONNECTION_ACCEPTED;
                NettyUtil.setClientId(ctx.channel(), clientId);
                ConnectManager.getInstance().putClient(clientId, clientSession);
            }
            if (returnCode != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                ctx.close();
                LogUtil.warn(log, "[CONNECT remote:{}] -> {} connect failure,returnCode={}", remoteAddress, clientId, returnCode);
                return;
            }
            MqttConnAckMessage ackMessage = MessageUtil.getConnectAckMessage(returnCode, sessionPresent, responseProperties);
            ctx.writeAndFlush(ackMessage);
            LogUtil.info(log, "1234[CONNECT remote:{}] -> {} connect to this mqtt server", remoteAddress, clientId);
            reConnect2SendMessage(clientId);
            clientSession.setUserName(userName);
            newClientNotify(clientSession);
        } catch (Exception ex) {
            LogUtil.warn(log, "[CONNECT remote:{}] -> Service Unavailable: cause={}", remoteAddress, ex);
            returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
            MqttConnAckMessage ackMessage = MessageUtil.getConnectAckMessage(returnCode, sessionPresent, null);
            ctx.writeAndFlush(ackMessage);
            ctx.close();
        }
    }

    protected void newClientNotify(ClientSession clientSession) {
        log.info("会话创建成功：{}", clientSession.getClientId());
    }

    private boolean keepAlive(String clientId, ChannelHandlerContext ctx, int heatbeatSec) {
        if (this.authValid.verifyHeartbeatTime(clientId, heatbeatSec)) {
            int keepAlive = (int) (heatbeatSec * 1.5f);
            if (ctx.pipeline().names().contains("idleStateHandler")) {
                ctx.pipeline().remove("idleStateHandler");
            }
            ctx.pipeline().addFirst("idleStateHandler", new IdleStateHandler(keepAlive, 0, 0));
            return true;
        }
        return false;
    }

    private void storeWillMsg(String clientId, boolean willRetain, int willQos,
                              String willTopic, byte[] willPayload, Map<Integer, Object> propertyMap) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageHeader.RETAIN, willRetain);
        headers.put(MessageHeader.QOS, willQos);
        headers.put(MessageHeader.TOPIC, willTopic);
        headers.put(MessageHeader.WILL, true);
        Message message = new Message(Message.Type.WILL, headers, willPayload);
        message.setProperties(propertyMap);
        message.setStoreTime(System.currentTimeMillis());
        message.setClientId(clientId);
        messageStore.storeWillMessage(clientId, message);
        if (willRetain) {
            messageStore.storeRetainMessage(willTopic, message);
        }
        LogUtil.info(log, "[WillMessageStore] : {} store will message:{}", clientId, message);
    }

    private ClientSession createNewClientSession(String clientId, int version, ChannelHandlerContext ctx) {
        ClientSession clientSession = new ClientSession(clientId, true, version, ctx);
        //clear previous sessions
        this.sessionStore.clearSession(clientId, true);
        return clientSession;
    }

    /**
     * cleanStart is false, reload client session
     */
    private ClientSession reloadClientSession(ChannelHandlerContext ctx, String clientId, int version) {
        ClientSession clientSession = new ClientSession(clientId, false, version, ctx);
        Set<Subscription> subscriptions = sessionStore.getSubscriptions(clientId);
        for (Subscription subscription : subscriptions) {
            this.subscriptionMatcher.subscribe(subscription);
        }
        return clientSession;
    }

    private void reConnect2SendMessage(String clientId) {
        this.reSendMessageService.put(clientId);
        this.reSendMessageService.wakeUp();
    }

    private boolean authentication(String clientId, String username, byte[] password, String defaultUser, String defaultPwd, boolean anonymousEnable) {
        return this.authValid.authentication(clientId, username, password, defaultUser, defaultPwd, anonymousEnable);
    }

    private boolean onBlackList(String remoteAddr, String clientId) {
        return this.authValid.onBlacklist(remoteAddr, clientId);
    }

    private boolean clientIdVerify(String clientId) {
        return this.authValid.clientIdVerify(clientId);
    }

    private boolean versionValid(int mqttVersion) {
        if (mqttVersion == 3 || mqttVersion == 4 || mqttVersion == 5) {
            return true;
        }
        return false;
    }

}
