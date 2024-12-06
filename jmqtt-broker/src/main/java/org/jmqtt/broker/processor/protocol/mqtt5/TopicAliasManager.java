package org.jmqtt.broker.processor.protocol.mqtt5;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/2 11:37
 */
@Slf4j
public class TopicAliasManager {

    private final static Map<String, Map<Integer, String>> TOPIC_ALIAS_MAP = new ConcurrentHashMap<>();

    public static void put(String clientId, Integer alias, String topic) {
        Map<Integer, String> aliasMap = TOPIC_ALIAS_MAP.get(clientId);
        if (aliasMap == null) {
            synchronized (clientId) {
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

    public static void remove(String clientId, Integer alias) {
        Map<Integer, String> aliasMap = TOPIC_ALIAS_MAP.get(clientId);
        if (aliasMap != null) {
            aliasMap.remove(alias);
        }
    }

    public static void clear(String clientId) {
        log.debug("client offline, clear topic alias.");
        TOPIC_ALIAS_MAP.remove(clientId);
    }

}
