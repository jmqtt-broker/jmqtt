package org.jmqtt.broker.common.config;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Getter;
import lombok.Setter;

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
    // 是否支持通配符订阅
    private Boolean wildcardSubscriptionAvailable = true;
    // 是否支持订阅标识符
    private Boolean subscriptionIdentifierAvailable = true;
    // 是否支持共享订阅
    private Boolean sharedSubscriptionAvailable = true;
    // 是否支持保留消息
    private Boolean retainAvailable = true;
    // 支持的最大QOS等级
    private Integer maximumQos = MqttQoS.EXACTLY_ONCE.value();

}
