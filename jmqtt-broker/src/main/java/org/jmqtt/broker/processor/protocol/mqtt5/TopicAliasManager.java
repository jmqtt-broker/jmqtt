package org.jmqtt.broker.processor.protocol.mqtt5;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.MessageHeader;
import org.jmqtt.broker.remoting.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TopicAliasManager {

    private final static Map<String, Map<Integer, String>> TOPIC_ALIAS_MAP = new ConcurrentHashMap<>();

    private final static Object LOCK = new Object();

    public static void put(String clientId, Integer alias, String topic) {
        Map<Integer, String> aliasMap = TOPIC_ALIAS_MAP.get(clientId);
        if (aliasMap == null) {
            synchronized (LOCK) {
                aliasMap = TOPIC_ALIAS_MAP.get(clientId);
                if (aliasMap == null) {
                    aliasMap = new ConcurrentHashMap<>();
                    TOPIC_ALIAS_MAP.put(clientId, aliasMap);
                }
            }
        }
        aliasMap.put(alias, topic);
    }

    public static String get(String clientId, Integer alias) {
        Map<Integer, String> aliasMap = TOPIC_ALIAS_MAP.get(clientId);
        if (aliasMap != null) {
            return aliasMap.get(alias);
        }
        return null;
    }

    public static String getRealTopic(Message message) {
        String topic = (String) message.getHeader(MessageHeader.TOPIC);
        if (StringUtils.isBlank(topic)) {
            Map<Integer, Object> properties = message.getProperties();
            if (properties != null && !properties.isEmpty()) {
                Integer topicAlias = (Integer) properties.get(MqttProperties.MqttPropertyType.TOPIC_ALIAS.value());
                topic = get(message.getClientId(), topicAlias);
            }
        }
        return topic;
    }

    public static void remove(String clientId, Integer alias) {
        Map<Integer, String> aliasMap = TOPIC_ALIAS_MAP.get(clientId);
        if (aliasMap != null) {
            aliasMap.remove(alias);
        }
    }

    public static void clear(String clientId) {
        TOPIC_ALIAS_MAP.remove(clientId);
    }

}
