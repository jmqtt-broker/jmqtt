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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class Mqtt5Utils {

    public static Map<Integer, Object> propertyMap(MqttProperties properties) {
        Map<Integer, Object> propertyMap = new HashMap<>();
        properties.listAll().forEach(p -> {
            if (p instanceof MqttProperties.BinaryProperty) {
                propertyMap.put(p.propertyId(), new String((byte[]) p.value()));
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

    public static boolean checkAlias(String clientId, Integer alias) {
        Integer maxAlias = (Integer) BrokerContext.getSessionStore().getClientProperty(clientId, MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM.value());
        return alias > 0 && maxAlias != null && alias <= maxAlias;
    }

    public static boolean checkPackageSize(ClientSession clientSession, int remainingLength) {
        boolean res = false;
        // 固定头长度2~5字节，这里直接以5为准
        int packetSize = remainingLength + 5;
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

}
