package com.jmqtt.core.netty.helper;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.AttributeKey;

import java.util.Optional;

public class NettyUtil {

    private static final AttributeKey<String> clientIdAttr = AttributeKey.valueOf("clientId");

    private static final AttributeKey<Integer> versionAttr = AttributeKey.valueOf("version");

    private static final AttributeKey<Boolean> mqtt5Attr = AttributeKey.valueOf("mqtt5");

    public static void setClientId(Channel channel,String clientId){
        channel.attr(clientIdAttr).set(clientId);
    }

    public static String getClientId(Channel channel){
        return channel.attr(clientIdAttr).get();
    }

    public static void setVersion(Channel channel, int version){
        channel.attr(versionAttr).set(version);
        channel.attr(mqtt5Attr).set(version == MqttVersion.MQTT_5.protocolLevel());
    }

    public static Integer getVersion(Channel channel) {
        return channel.attr(versionAttr).get();
    }

    public static boolean isMqtt5(Channel channel){
        return Optional.ofNullable(channel.attr(mqtt5Attr).get()).orElse(false);
    }
}
