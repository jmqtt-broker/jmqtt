package org.jmqtt.broker.processor.protocol.mqtt5;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.util.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Mqtt5Utils {

    public static Map<Integer, Object> propertyMap(MqttProperties properties) {
        Map<Integer, Object> propertyMap = new HashMap<>();
        properties.listAll().forEach(p -> {
            if (p instanceof MqttProperties.BinaryProperty) {
                propertyMap.put(p.propertyId(), new String((byte[]) p.value()));
            } else if (p instanceof MqttProperties.UserProperties) {
                List<MqttProperties.StringPair> value = ((MqttProperties.UserProperties) p).value();
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

    public static boolean isOk(byte reasonCode) {
        return reasonCode == 0x00;
    }

    public static int timeoutSecond(String clientId) {
        Integer expire = (Integer) BrokerContext.getSessionStore()
                .getClientProperty(clientId, MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL.value());
        return Optional.ofNullable(expire).orElse(0);
    }

    public static Set<Subscription> getSubscriptions(String topic, String clientId) {
        return BrokerContext.getSubscriptionMatcher().match(topic, clientId);
    }

    public static void sendDisconnectAndClose(ClientSession session, byte reasonCode) {
        ChannelHandlerContext ctx = session.getCtx();
        if (session.isMqtt5()) {
            ctx.writeAndFlush(MessageUtil.getDisconnectMessage(reasonCode));
        }
        ctx.close();
    }

    public static void sendDisconnectAndClose(ClientSession session, byte reasonCode, String reasonString) {
        ChannelHandlerContext ctx = session.getCtx();
        if (session.isMqtt5()) {
            ctx.writeAndFlush(MessageUtil.getDisconnectMessage(reasonCode, reasonString));
        }
        ctx.close();
    }

    public static boolean checkAlias(String clientId, Integer alias) {
        Integer maxAlias = (Integer) BrokerContext.getSessionStore().getClientProperty(clientId, MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM.value());
        return alias > 0 && maxAlias != null && alias <= maxAlias;
    }

    public static boolean checkPackageSize(ClientSession clientSession, int remainingLength) {
        boolean res = false;
        // 固定头长度2~5字节，这里直接以5为准
        int packetSize = getMsgSize(remainingLength);
        String clientId = clientSession.getClientId();
        Integer maxSize = (Integer) BrokerContext.getSessionStore().getClientProperty(clientId, MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE.value());
        if (maxSize != null) {
            if (packetSize > maxSize) {
                // 当包大小大于连接时约定的值时，服务端主动发送DISCONNECT
                log.warn("client max packet size error,clientId {}", clientId);
                res = true;
            }
        }
        if (packetSize > BrokerContext.getBrokerConfig().getMaximumPacketSize()) {
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
     * @param remainingLength
     * @return
     */
    public static int getMsgSize(int remainingLength) {
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
