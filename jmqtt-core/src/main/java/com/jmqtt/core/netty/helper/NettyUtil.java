package com.jmqtt.core.netty.helper;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class NettyUtil {

    private static final AttributeKey<String> clientIdAttr = AttributeKey.valueOf("clientId");

    public static void setClientId(Channel channel,String clientId){
        channel.attr(clientIdAttr).set(clientId);
    }

    public static String getClientId(Channel channel){
        return channel.attr(clientIdAttr).get();
    }
}
