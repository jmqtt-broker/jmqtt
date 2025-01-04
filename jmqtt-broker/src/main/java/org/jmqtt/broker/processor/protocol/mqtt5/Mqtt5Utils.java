package org.jmqtt.broker.processor.protocol.mqtt5;

import io.netty.handler.codec.mqtt.MqttProperties;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.MessageHeader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/11 17:11
 */
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

}
