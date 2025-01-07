package org.jmqtt.broker.common.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NettyConfig {
    private int tcpBackLog = 1024;
    private Boolean tcpNoDelay = false;
    private Boolean tcpReuseAddr = true;
    private Boolean tcpKeepAlive = false;
    private int tcpSndBuf = 65536;
    private int tcpRcvBuf = 65536;
    private Boolean useEpoll = false;
    private Boolean pooledByteBufAllocatorEnable = false;

    /**
     * tcp port default 1883
     */
    private Boolean startTcp = true;
    private int tcpPort = 1883;

    /**
     * websocket port default 1884
     */
    private Boolean startWebsocket = true;
    private int websocketPort = 1884;

    /**
     * http port default 1881
     */
    private Boolean startHttp = true;
    private int httpPort = 1881;

    /**
     * tcp port with ssl default 8883
     */
    private Boolean startSslTcp = true;
    private int sslTcpPort = 8883;

    /**
     * websocket port with ssl default 8884
     */
    private Boolean startSslWebsocket = true;
    private int sslWebsocketPort = 8884;

    /**
     * SSL setting
     */
    private Boolean useClientCA = false;
    private String sslKeyStoreType = "PKCS12";
    private String sslKeyFilePath = "conf/server.pfx";
    private String sslManagerPwd = "654321";
    private String sslStorePwd = "654321";
    /**
     * max mqtt message size
     */
    private int maxMsgSize = 256 * 1024;

}
