package org.jmqtt.broker.processor.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
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

    public PublishProcessor(BrokerController controller) {
        super(controller);
        this.authValid = controller.getAuthValid();
        this.sessionStore = controller.getSessionStore();
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        try {
            MqttPublishMessage publishMessage = (MqttPublishMessage) mqttMessage;
            MqttFixedHeader fixedHeader = publishMessage.fixedHeader();
            String clientId = NettyUtil.getClientId(ctx.channel());
            ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
            if (clientSession.isMqtt5() && checkPackageSize(clientSession, fixedHeader.remainingLength())) {
                // 消息超出约定的大小，直接丢弃
                log.warn("exceeding message, clientId: {}", clientId);
                clientSession.getCtx().close();
                return;
            }
            MqttPublishVariableHeader variableHeader = publishMessage.variableHeader();
            MqttQoS qos = fixedHeader.qosLevel();
            Message innerMsg = new Message();
            innerMsg.setStoreTime(System.currentTimeMillis());
            String topic = variableHeader.topicName();
            if (!this.authValid.publishVerify(clientId, topic)) {
                LogUtil.warn(log, "[PubMessage] permission is not allowed");
                clientSession.getCtx().close();
                return;
            }
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
            innerMsg.setMsgId(variableHeader.packetId());
            switch (qos) {
                case AT_MOST_ONCE:
                    processMessage(innerMsg);
                    break;
                case AT_LEAST_ONCE:
                    processQos1(ctx, innerMsg);
                    break;
                case EXACTLY_ONCE:
                    processQos2(ctx, innerMsg);
                    break;
                default:
                    LogUtil.warn(log, "[PubMessage] -> Wrong mqtt message,clientId={}", clientId);
            }
        } catch (Throwable tr) {
            LogUtil.error(log, "[PubMessage] -> Solve mqtt pub message exception:{}", tr.getMessage());
        } finally {
            ReferenceCountUtil.release(mqttMessage.payload());
        }
    }

    private void processQos2(ChannelHandlerContext ctx, Message innerMsg) {
        int originMessageId = innerMsg.getMsgId();
        LogUtil.debug(log, "[PubMessage] -> Process qos2 message,clientId={}", innerMsg.getClientId());
        boolean flag = cacheInflowMsg(innerMsg.getClientId(), innerMsg);
        if (!flag) {
            LogUtil.warn(log, "[PubMessage] -> cache qos2 pub message failure,clientId={}", innerMsg.getClientId());
        }
        MqttMessage pubRecMessage = MessageUtil.getPubRecMessage(originMessageId);
        ctx.writeAndFlush(pubRecMessage);
    }

    private void processQos1(ChannelHandlerContext ctx, Message innerMsg) {
        int originMessageId = innerMsg.getMsgId();
        processMessage(innerMsg);
        LogUtil.info(log, "[PubMessage] -> Process qos1 message,clientId={}", innerMsg.getClientId());
        MqttPubAckMessage pubAckMessage = MessageUtil.getPubAckMessage(originMessageId);
        ctx.writeAndFlush(pubAckMessage);
    }

    private boolean checkPackageSize(ClientSession clientSession, int remainingLength) {
        boolean res = false;
        String clientId = clientSession.getClientId();
        Integer maxSize = (Integer) sessionStore.getClientProperty(clientId, MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE.value());
        if (maxSize != null) {
            if ((remainingLength + 5) > maxSize) {
                // 固定头长度2~5字节，这里直接以5为准
                // 当包大小大于连接时约定的值时，服务端主动发送DISCONNECT
                clientSession.getCtx().writeAndFlush(MessageUtil.getDisconnectMessage((byte) 0x95));
                log.warn("max packet size error,clientId {}", clientId);
                res = true;
            }
        }
        return res;
    }

}
