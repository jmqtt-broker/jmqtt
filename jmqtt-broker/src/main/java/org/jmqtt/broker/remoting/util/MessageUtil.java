package org.jmqtt.broker.remoting.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.common.model.SubscriptionOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * transfer message from Message and MqttMessage
 */
public class MessageUtil {

    public static byte[] readBytesFromByteBuf(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    public static byte[] getBytesFromByteBuf(ByteBuf byteBuf) {
        ByteBuf newBuf = byteBuf.copy();
        byte[] bytes = new byte[newBuf.readableBytes()];
        newBuf.readBytes(bytes);
        return bytes;
    }

    public static MqttUnsubAckMessage getUnSubAckMessage(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        return new MqttUnsubAckMessage(fixedHeader, idVariableHeader);
    }

    public static int getMessageId(MqttMessage mqttMessage) {
        MqttMessageIdVariableHeader idVariableHeader = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        return idVariableHeader.messageId();
    }

    public static int getMinQos(int qos1, int qos2) {
        return Math.min(qos1, qos2);
    }

    public static MqttMessage getPubRelMessage(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        return new MqttMessage(fixedHeader, idVariableHeader);
    }

    public static MqttPublishMessage getPubMessage(Message message, boolean dup) {
        return getPubMessage(message, dup, null);
    }

    public static MqttPublishMessage getPubMessage(Message message, boolean dup, SubscriptionOption option) {
        boolean retain = (boolean) Optional.ofNullable(message.getHeader(MessageHeader.RETAIN)).orElse(false);
        int remainingLength = (int) Optional.ofNullable(message.getHeader(MessageHeader.REMAINING_LENGTH)).orElse(0);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                dup,
                MqttQoS.valueOf((int) message.getHeader(MessageHeader.QOS)),
                option != null && option.isRetainAsPublished() && retain,
                remainingLength);
        MqttProperties properties = getProperties(message.getProperties());
        if (properties != null && option != null) {
            properties.add(new MqttProperties.IntegerProperty(
                    MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value(),
                    option.getSubscriptionIdentifier()));
        }
        MqttPublishVariableHeader publishVariableHeader = new MqttPublishVariableHeader(
                (String) message.getHeader(MessageHeader.TOPIC), message.getMsgId(), properties);
        ByteBuf heapBuf;
        if (message.getPayload() == null) {
            heapBuf = Unpooled.EMPTY_BUFFER;
        } else {
            heapBuf = Unpooled.wrappedBuffer(message.getPayload());
        }
        return new MqttPublishMessage(fixedHeader, publishVariableHeader, heapBuf);
    }

    private static MqttProperties getProperties(Map<Integer, Object> propertyMap) {
        if (propertyMap != null && !propertyMap.isEmpty()) {
            MqttProperties properties = new MqttProperties();
            propertyMap.forEach((k, v) -> {
                if (k == MqttProperties.MqttPropertyType.USER_PROPERTY.value()) {
                    if (v instanceof ArrayList) {
                        ((ArrayList) v).forEach(p -> {
                            MqttProperties.StringPair u = (MqttProperties.StringPair) p;
                            properties.add(new MqttProperties.UserProperty(u.key, u.value));
                        });
                    } else if (v instanceof JSONArray) {
                        ((JSONArray) v).forEach(pair -> {
                            ((JSONObject) pair).forEach((key, val) -> {
                                properties.add(new MqttProperties.UserProperty(key, val.toString()));
                            });
                        });
                    }
                } else if (k == MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value()) {
                    properties.add(new MqttProperties.IntegerProperty(k, (int) v));
                } else if (k == MqttProperties.MqttPropertyType.CORRELATION_DATA.value()) {
                    properties.add(new MqttProperties.BinaryProperty(k, ((String) v).getBytes()));
                } else if (v instanceof String) {
                    properties.add(new MqttProperties.StringProperty(k, (String) v));
                } else if (v instanceof Integer) {
                    properties.add(new MqttProperties.IntegerProperty(k, (int) v));
                }
            });
            return properties;
        }
        return null;
    }

    public static Object getProperty(Message message, Integer propertyId) {
        Map<Integer, Object> properties = message.getProperties();
        if (properties != null && !properties.isEmpty()) {
            return properties.get(propertyId);
        }
        return null;
    }

    public static MqttMessage getSubAckMessage(int messageId, List<Integer> qos) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttSubAckPayload subAckPayload = new MqttSubAckPayload(qos);
        return new MqttSubAckMessage(fixedHeader, idVariableHeader, subAckPayload);
    }

    public static MqttMessage getPingRespMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader);
        return mqttMessage;
    }

    public static MqttMessage getPubComMessage(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader, MqttMessageIdVariableHeader.from(messageId));
        return mqttMessage;
    }

    public static MqttMessage getPubRecMessage(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader, MqttMessageIdVariableHeader.from(messageId));
        return mqttMessage;
    }

    public static MqttMessage getPubRecMessage(int messageId, boolean isDup) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, isDup, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessage mqttMessage = new MqttMessage(fixedHeader, MqttMessageIdVariableHeader.from(messageId));
        return mqttMessage;
    }

    public static MqttPubAckMessage getPubAckMessage(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        return new MqttPubAckMessage(fixedHeader, idVariableHeader);
    }

    public static MqttConnAckMessage getConnectAckMessage(MqttConnectReturnCode returnCode,
                                                          boolean sessionPresent,
                                                          MqttProperties properties) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.EXACTLY_ONCE, false, 0);
        MqttConnAckVariableHeader variableHeader;
        if (properties != null && !properties.isEmpty()) {
            variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent, properties);
        } else {
            variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        }
        return new MqttConnAckMessage(fixedHeader, variableHeader);
    }
}
