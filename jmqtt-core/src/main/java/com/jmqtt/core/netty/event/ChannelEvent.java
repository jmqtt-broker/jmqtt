package com.jmqtt.core.netty.event;

import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public class ChannelEvent {

    private String remoteAddr;
    private NettyEventType eventType;
    private Channel channel;

    public ChannelEvent(String remoteAddr, NettyEventType eventType, Channel channel) {
        this.remoteAddr = remoteAddr;
        this.eventType = eventType;
        this.channel = channel;
    }

    public static ChannelEvent of(String remoteAddr, NettyEventType eventType, Channel channel) {
        return new ChannelEvent(remoteAddr, eventType, channel);
    }

}
