package com.jmqtt.core.mqtt.helper;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.jmqtt.core.JmqttContext;
import com.jmqtt.core.mqtt.model.Message;
import com.jmqtt.core.mqtt.model.MessageHeader;
import com.jmqtt.core.mqtt.model.SubscriptionOption;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * transfer message from Message and MqttMessage
 */
public class MessageUtil {

    public static byte[] readBytesFromByteBuf(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return bytes;
    }

    public static MqttUnsubAckMessage getUnSubAckMessage(int messageId, byte reasonCode) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        if (reasonCode != 0) {
            return new MqttUnsubAckMessage(fixedHeader, idVariableHeader, new MqttUnsubAckPayload(reasonCode));
        }
        return new MqttUnsubAckMessage(fixedHeader, idVariableHeader);
    }

    public static int getMessageId(MqttMessage mqttMessage) {
        MqttMessageIdVariableHeader idVariableHeader = (MqttMessageIdVariableHeader) mqttMessage.variableHeader();
        return idVariableHeader.messageId();
    }

    public static int getMinQos(int qos1, int qos2) {
        return Math.min(qos1, qos2);
    }

    public static MqttPublishMessage getPubMessage(Message message, boolean dup) {
        return getPubMessage(message, dup, null, null);
    }

    public static MqttPublishMessage getPubMessage(Message message, boolean dup, SubscriptionOption option, String subClientId) {
        boolean retain = (boolean) Optional.ofNullable(message.getHeader(MessageHeader.RETAIN)).orElse(false);
        int remainingLength = (int) Optional.ofNullable(message.getHeader(MessageHeader.REMAINING_LENGTH)).orElse(0);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                dup,
                MqttQoS.valueOf((int) message.getHeader(MessageHeader.QOS)),
                option != null && option.getRetainAsPublished() && retain,
                remainingLength);
        MqttProperties properties = getProperties(message.getProperties(), subClientId);
        if (properties != null && option != null) {
            Optional.ofNullable(option.getSubscriptionIdentifier()).ifPresent(subscriptId -> {
                properties.add(new MqttProperties.IntegerProperty(
                        MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value(),
                        subscriptId));
            });
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

    private static MqttProperties getProperties(Map<Integer, Object> propertyMap, String subClientId) {
        if (propertyMap != null && !propertyMap.isEmpty()) {
            MqttProperties properties = new MqttProperties();
            propertyMap.forEach((k, v) -> {
                if (k == MqttProperties.MqttPropertyType.USER_PROPERTY.value()) {
                    if (v instanceof ArrayList) {
                        ((ArrayList) v).forEach(p -> {
                            Map<String, String> u = (HashMap<String, String>) p;
                            properties.add(new MqttProperties.UserProperty(u.get("key"), u.get("value")));
                        });
                    } else if (v instanceof JSONArray) {
                        ((JSONArray) v).forEach(pair -> {
                            JSONObject pairObj = ((JSONObject) pair);
                            properties.add(new MqttProperties.UserProperty(pairObj.getStr("key"), pairObj.getStr("value")));
                        });
                    }
                } else if (k == MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER.value()) {
                    properties.add(new MqttProperties.IntegerProperty(k, (int) v));
                } else if (k == MqttProperties.MqttPropertyType.CORRELATION_DATA.value()) {
                    properties.add(new MqttProperties.BinaryProperty(k, ((String) v).getBytes()));
                } else if (v instanceof String) {
                    properties.add(new MqttProperties.StringProperty(k, (String) v));
                } else if (v instanceof Integer) {
                    if (StrUtil.isNotBlank(subClientId) && k == MqttProperties.MqttPropertyType.TOPIC_ALIAS.value()) {
                        // 发布的消息带有主题别名，订阅方建立连接的时候如果没有设置别名最大值，
                        // 那么转发消息的时候需要将消息中的主题别名去掉
                        Optional.ofNullable(JmqttContext.getSessionStore().getClientProperty(subClientId,
                                MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM.value())).ifPresent(aliasMax -> {
                            properties.add(new MqttProperties.IntegerProperty(k, (int) v));
                        });
                    } else {
                        properties.add(new MqttProperties.IntegerProperty(k, (int) v));
                    }
                }
            });
            return properties;
        }
        return null;
    }

    public static MqttMessage getSubAckMessage(int messageId, List<Integer> qos) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader idVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttSubAckPayload subAckPayload = new MqttSubAckPayload(qos);
        return new MqttSubAckMessage(fixedHeader, idVariableHeader, subAckPayload);
    }

    public static MqttMessage getPingRespMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        return new MqttMessage(fixedHeader);
    }

    public static MqttMessage getPubReplyMessage(int messageId, MqttMessageType type,
                                                 byte reasonCode, MqttProperties properties, boolean isDup) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(type, isDup, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttPubReplyMessageVariableHeader variableHeader = new MqttPubReplyMessageVariableHeader(messageId, reasonCode, properties);
        return new MqttMessage(fixedHeader, variableHeader);
    }

    public static MqttConnAckMessage getConnectAckMessage(MqttConnectReturnCode returnCode,
                                                          boolean sessionPresent,
                                                          MqttProperties properties) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader variableHeader;
        if (properties != null && !properties.isEmpty()) {
            variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent, properties);
        } else {
            variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        }
        return new MqttConnAckMessage(fixedHeader, variableHeader);
    }

    public static MqttMessage getDisconnectMessage(byte reasonCode) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttReasonCodeAndPropertiesVariableHeader variableHeader = new MqttReasonCodeAndPropertiesVariableHeader(reasonCode, null);
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.REASON_STRING.value(), ""));
        return new MqttMessage(fixedHeader, variableHeader);
    }

    public static MqttMessage getDisconnectMessage(Reason reason) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttProperties properties = new MqttProperties();
        properties.add(new MqttProperties.StringProperty(MqttProperties.MqttPropertyType.REASON_STRING.value(), reason.getReasonString()));
        MqttReasonCodeAndPropertiesVariableHeader variableHeader = new MqttReasonCodeAndPropertiesVariableHeader(reason.getReasonCode(), properties);
        return new MqttMessage(fixedHeader, variableHeader);
    }

    public static void sendDisconnect(ClientSession session, Reason reason) {
        ChannelHandlerContext ctx = session.getCtx();
        if (session.isMqtt5()) {
            ctx.writeAndFlush(MessageUtil.getDisconnectMessage(reason)).addListener(future -> {
                if (future.isDone() && ctx.channel().isActive()) {
                    ctx.close();
                }
            });
        } else {
            ctx.close();
        }
    }
    public static Map<Integer, Object> propertyMap(MqttProperties properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        Map<Integer, Object> propertyMap = new HashMap<>();
        properties.listAll().forEach(p -> {
            if (p instanceof MqttProperties.BinaryProperty) {
                propertyMap.put(p.propertyId(), new String((byte[]) p.value()));
            } else if (p instanceof MqttProperties.UserProperties userProperties) {
                List<MqttProperties.StringPair> value = userProperties.value();
                List<Map<String, String>> kvList = value.stream().map(pair -> {
                    Map<String, String> kv = new HashMap<>();
                    kv.put("key", pair.key);
                    kv.put("value", pair.value);
                    return kv;
                }).collect(Collectors.toList());
                if (!kvList.isEmpty()) {
                    propertyMap.put(p.propertyId(), kvList);
                }
            } else {
                propertyMap.put(p.propertyId(), p.value());
            }
        });
        return propertyMap;
    }

}
