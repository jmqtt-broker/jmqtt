package com.jmqtt.core.netty.helper;


import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
public class ChannelHelper {

    public static String getRemoteAddr(Channel channel) {
        if (channel != null) {
            SocketAddress remote = channel.remoteAddress();
            String addr = remote != null ? remote.toString() : "";
            if (!addr.isEmpty()) {
                int index = addr.lastIndexOf("/");
                if (index >= 0) {
                    return addr.substring(index + 1);
                }
                return addr;
            }
        }
        return "";
    }

}
