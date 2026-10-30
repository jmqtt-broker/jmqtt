package com.jmqtt.core.netty.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NettyConfig {
    private int tcpBackLog = 1024;
    private Boolean noDelay = true;
    private Boolean soKeepAlive = true;
    private int tcpSndBuf = 65536;
    private int tcpRcvBuf = 65536;
    private Boolean useEpoll = true;
    private Boolean pooledByteBufAllocatorEnable = true;

    private Boolean enableTcp = true;
    private int tcpPort = 1883;

    private Boolean enableWebsocket = true;
    private int websocketPort = 1884;

    private Boolean enableSslTcp = true;
    private int sslTcpPort = 8883;

    private Boolean enableSslWebsocket = true;
    private int sslWebsocketPort = 8884;

    private TLS tls;

}
