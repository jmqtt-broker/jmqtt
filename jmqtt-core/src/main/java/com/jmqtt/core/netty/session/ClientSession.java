package com.jmqtt.core.netty.session;

import com.jmqtt.core.mqtt.JmqttContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.AttributeKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@EqualsAndHashCode(of = {"clientId"})
@NoArgsConstructor
public class ClientSession {

    private String clientId;
    private int version;
    private boolean cleanStart;
    private transient ChannelHandlerContext ctx;

    private transient AtomicInteger messageIdCounter = new AtomicInteger(1);

    public ClientSession(String clientId, boolean cleanStart, int version, ChannelHandlerContext ctx) {
        this.clientId = clientId;
        this.cleanStart = cleanStart;
        this.version = version;
        this.ctx = ctx;
    }

    public int generateMessageId() {
        int messageId = messageIdCounter.getAndIncrement();
        messageId = Math.abs(messageId % 0xFFFF);
        if (messageId == 0) {
            return generateMessageId();
        }
        return messageId;
    }

    public boolean isMqtt5() {
        return this.version == MqttVersion.MQTT_5.protocolLevel();
    }

    public boolean normalDisconnection() {
        Boolean normalDisconnection = (Boolean) ctx.channel().attr(AttributeKey.valueOf("NORMAL_DISCONNECTION")).get();
        return normalDisconnection != null && normalDisconnection;
    }

    public boolean publishWill() {
        Boolean publishWill = (Boolean) ctx.channel().attr(AttributeKey.valueOf("PUBLISH_WILL")).get();
        return publishWill != null && publishWill;
    }

    /**
     * 客户端会话超时时间（秒）
     *
     * @return
     */
    public int timeoutSecond() {
        if (isMqtt5()) {
            Integer expire = (Integer) JmqttContext.getSessionStore()
                    .getClientProperty(clientId, MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL.value());
            return expire != null ? expire : JmqttContext.getJmqttConfig().getBroker().getDefaultSessionTimeout();
        } else {
            // cleanStart=false时：
            // MQTT各版本规范中，没有对mqtt3、3.1.1的客户端明确要求过期时间，很多broker没有实现对这类客户端的处理，
            // 造成离线的客户端会话永久存在于broker存储资源中。
            // 所以对这类离线的客户端，设置一个默认时间，可配置。
            return JmqttContext.getJmqttConfig().getBroker().getDefaultSessionTimeout();
        }
    }

}
