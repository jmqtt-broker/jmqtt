package com.jmqtt.core.mqtt.config;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BrokerConfig {

    private Boolean anonymous = false;
    private String user = "jmqtt";
    private String password = "jmqtt";


    // 是否使用服务端默认的心跳周期
    private boolean useServerKeepalive = false;
    private int defaultKeepalive = 60;
    // 服务端能同时处理的非qos0最大消息数
    private Integer receiveMaximum = 65535;
    // 服务端能处理的最大packet长度，默认256M，也是mqtt报文数据最大长度（可变头 + payload）
    private Integer maximumPacketSize = 1024 * 1024 * 256;
    // 主题别名最大值
    private Integer topicAliasMaximum = 65535;
    // 服务端生成的clientId默认前缀
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
