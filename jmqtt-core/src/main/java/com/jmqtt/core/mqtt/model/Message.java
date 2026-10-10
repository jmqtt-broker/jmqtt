package com.jmqtt.core.mqtt.model;

import io.netty.handler.codec.mqtt.MqttProperties;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"msgId"})
public class Message {

    private int msgId;

    private Map<String, Object> headers = new HashMap<>();

    private Map<Integer, Object> properties = new HashMap<>();

    private String clientId;

    private byte[] payload;

    private long storeTime;

    public Message(Map<String, Object> headers, byte[] payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public Object putHeader(String key, Object value) {
        return headers.put(key, value);
    }

    public Object removeHeader(String key) {
        return headers.remove(key);
    }

    public Object getHeader(String key) {
        return headers.get(key);
    }

    /**
     * 是否为时效性消息，包含过期间隔属性的消息为时效性消息，转发的时候
     * 需要携带有效时间，剩余有效时间 = 有效间隔 - 服务器停留时间
     *
     * @return true or false
     */
    public boolean validity() {
        if (properties != null && !properties.isEmpty()) {
            Integer expire = (Integer) properties.get(MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL.value());
            return expire != null && expire > 0;
        }
        return false;
    }

    /**
     * 剩余有效时间 = 有效间隔 - 服务器停留时间
     *
     * @return int
     */
    public int alive() {
        if (properties != null && !properties.isEmpty()) {
            Integer exipre = (Integer) properties.get(MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL.value());
            if (exipre != null) {
                int aliveSeconds = (int) ((storeTime / 1000 + exipre) - System.currentTimeMillis() / 1000);
                if (aliveSeconds > 0) {
                    properties.put(MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL.value(), aliveSeconds);
                    return aliveSeconds;
                }
            }
        }
        return 0;
    }

}
