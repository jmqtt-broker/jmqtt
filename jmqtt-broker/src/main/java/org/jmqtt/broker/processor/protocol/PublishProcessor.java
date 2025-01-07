package org.jmqtt.broker.processor.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.processor.protocol.mqtt5.TopicAliasManager;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.store.SessionStore;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端publish消息到jmqtt broker
 * TODO mqtt5实现,流控处理
 */
public class PublishProcessor extends AbstractMessageProcessor implements RequestProcessor {
    private Logger log = JmqttLogger.messageTraceLog;

    private AuthValid authValid;

    private SessionStore sessionStore;

    private BrokerConfig brokerConfig;

    public PublishProcessor(BrokerController controller) {
        super(controller);
        this.authValid = controller.getAuthValid();
        this.sessionStore = controller.getSessionStore();
        this.brokerConfig = controller.getBrokerConfig();
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        try {
            byte reasonCode = 0x00;
            MqttPublishMessage publishMessage = (MqttPublishMessage) mqttMessage;
            MqttFixedHeader fixedHeader = publishMessage.fixedHeader();
            String clientId = NettyUtil.getClientId(ctx.channel());
            MqttQoS qos = fixedHeader.qosLevel();
            ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
            if (clientSession.isMqtt5()) {
                if (!brokerConfig.getRetainAvailable() && fixedHeader.isRetain()) {
                    // broker没有开启保留消息功能，禁止发布保留消息
                    log.warn("retain not available, clientId: {}", clientId);
                    Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x9A);
                    return;
                } else if (qos.value() > brokerConfig.getMaximumQos()) {
                    log.warn("QoS not supported, clientId: {}", clientId);
                    Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x9B);
                    return;
                } else if (Mqtt5Utils.checkPackageSize(clientSession, fixedHeader.remainingLength())) {
                    // 消息超出约定的大小
                    log.warn("exceeding message, clientId: {}", clientId);
                    Mqtt5Utils.sendDisconnectAndClose(clientSession, (byte) 0x95);
                    return;
                }
            }
            MqttPublishVariableHeader variableHeader = publishMessage.variableHeader();
            Message innerMsg = new Message();
            innerMsg.setStoreTime(System.currentTimeMillis());
            innerMsg.setMsgId(variableHeader.packetId());
            String topic = variableHeader.topicName();
            if (!this.authValid.publishVerify(clientId, topic)) {
                LogUtil.warn(log, "[PubMessage] permission is not allowed");
                reasonCode = (byte) 0x87;
            }
            boolean isOk = Mqtt5Utils.isOk(reasonCode);
            if (isOk) {
                innerMsg.setPayload(MessageUtil.readBytesFromByteBuf(publishMessage.payload()));
                innerMsg.setClientId(clientId);
                innerMsg.setType(Message.Type.valueOf(fixedHeader.messageType().value()));
                Map<String, Object> headers = new HashMap<>();
                headers.put(MessageHeader.TOPIC, topic);
                headers.put(MessageHeader.QOS, fixedHeader.qosLevel().value());
                headers.put(MessageHeader.RETAIN, fixedHeader.isRetain());
                headers.put(MessageHeader.DUP, fixedHeader.isDup());
                headers.put(MessageHeader.REMAINING_LENGTH, fixedHeader.remainingLength());
                if (clientSession.isMqtt5()) {
                    MqttProperties properties = variableHeader.properties();
                    if (properties != null && !properties.isEmpty()) {
                        MqttProperties.IntegerProperty topicAlias = (MqttProperties.IntegerProperty)
                                properties.getProperty(MqttProperties.MqttPropertyType.TOPIC_ALIAS.value());
                        if (topicAlias != null) {
                            if (StringUtils.isNotBlank(topic)) {
                                TopicAliasManager.put(clientId, topicAlias.value(), topic);
                            } else {
                                headers.put(MessageHeader.TOPIC, TopicAliasManager.get(clientId, topicAlias.value()));
                            }
                        }
                        innerMsg.setProperties(Mqtt5Utils.propertyMap(properties));
                    }
                }
                innerMsg.setHeaders(headers);
            }
            switch (qos) {
                case AT_MOST_ONCE:
                    if (isOk) {
                        processMessage(innerMsg);
                    }
                    break;
                case AT_LEAST_ONCE:
                    processQos1(ctx, innerMsg, reasonCode);
                    break;
                case EXACTLY_ONCE:
                    processQos2(ctx, innerMsg, reasonCode);
                    break;
                default:
                    LogUtil.warn(log, "[PubMessage] -> Wrong mqtt message,clientId={}", clientId);
            }
            if (!isOk && !clientSession.isMqtt5()) {
                ctx.close();
            }
        } catch (Throwable tr) {
            LogUtil.error(log, "[PubMessage] -> Solve mqtt pub message exception:{}", tr);
        } finally {
            ReferenceCountUtil.release(mqttMessage.payload());
        }
    }

    private void processQos2(ChannelHandlerContext ctx, Message innerMsg, byte reasonCode) {
        int originMessageId = innerMsg.getMsgId();
        if (Mqtt5Utils.isOk(reasonCode)) {
            /*if (hasSubscriber(innerMsg)) {
                reasonCode = 0x10;
            }*/
            LogUtil.debug(log, "[PubMessage] -> Process qos2 message,clientId={}", innerMsg.getClientId());
            boolean flag = cacheInflowMsg(innerMsg.getClientId(), innerMsg);
            if (!flag) {
                LogUtil.warn(log, "[PubMessage] -> cache qos2 pub message failure,clientId={}", innerMsg.getClientId());
            }
        }
        // MqttMessage pubRecMessage = MessageUtil.getPubRecMessage(originMessageId);
        MqttMessage pubRecMessage = MessageUtil.getPubReplyMessage(originMessageId, MqttMessageType.PUBREC, reasonCode, null);
        ctx.writeAndFlush(pubRecMessage);
    }

    private void processQos1(ChannelHandlerContext ctx, Message innerMsg, byte reasonCode) {
        int originMessageId = innerMsg.getMsgId();
        if (Mqtt5Utils.isOk(reasonCode)) {
            /*if (hasSubscriber(innerMsg)) {
                reasonCode = 0x10;
            }*/
            processMessage(innerMsg);
            LogUtil.info(log, "[PubMessage] -> Process qos1 message,clientId={}", innerMsg.getClientId());
        }
        // MqttPubAckMessage pubAckMessage = MessageUtil.getPubAckMessage(originMessageId);
        MqttMessage pubAckMessage = MessageUtil.getPubReplyMessage(originMessageId, MqttMessageType.PUBACK, reasonCode, null);
        ctx.writeAndFlush(pubAckMessage);
    }

    private boolean hasSubscriber(Message message) {
        LogUtil.warn(log, "[PubMessage] -> No matching subscribers,clientId={}", message.getClientId());
        return !Mqtt5Utils.getSubscriptions((String) message.getHeader(MessageHeader.TOPIC), message.getClientId()).isEmpty();
    }

}
