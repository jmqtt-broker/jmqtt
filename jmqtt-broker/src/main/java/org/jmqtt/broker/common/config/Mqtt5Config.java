package org.jmqtt.broker.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/12/31 16:57
 */
@Getter
@Setter
public class Mqtt5Config {

    // 是否使用服务端默认的心跳周期
    private Boolean useServerKeepalive = false;
    private Integer defaultKeepalive = 60;

    // 服务端能同时处理的非qos0最大消息数
    private Integer receiveMaximum = 65535;
    // 服务端能处理的最大packet长度，默认256M
    private Integer maximumPacketSize = 1024 * 1024 * 256;
    // 主题别名最大值
    private Integer topicAliasMaximum;
    // 默认clientId前缀，当客户端未设置clientId时，由服务端生成clientId返回
    private String clientIdPrefix = "JMQTT_CLIENT_ID_";

}
