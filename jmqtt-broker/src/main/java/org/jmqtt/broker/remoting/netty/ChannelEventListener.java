package org.jmqtt.broker.remoting.netty;

import io.netty.channel.Channel;

public interface ChannelEventListener {

    /**
     * channel connect event
     * @param remoteAddr    客户端地址
     * @param channel       消息通道
     */
    void onChannelConnect(String remoteAddr, Channel channel);

    /**
     * channel close
     * @param remoteAddr    客户端地址
     * @param channel       消息通道
     */
    void onChannelClose(String remoteAddr,Channel channel);

    /**
     * channel heartbeat over time
     * @param remoteAddr    客户端地址
     * @param channel       消息通道
     */
    void onChannelIdle(String remoteAddr,Channel channel);

    /**
     * channel exception
     * @param remoteAddr    客户端地址
     * @param channel       消息通道
     */
    void onChannelException(String remoteAddr,Channel channel);
}
