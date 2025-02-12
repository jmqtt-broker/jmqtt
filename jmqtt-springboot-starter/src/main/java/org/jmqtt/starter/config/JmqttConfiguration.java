package org.jmqtt.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.jmqtt.broker.common.JmqttConst;
import org.jmqtt.broker.common.config.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jmqtt.broker")
@Getter
@Setter
public class JmqttConfiguration {

    private Boolean anonymousEnable = false;
    private String user = "admin";
    private String pwd = "admin";
    private Boolean highPerformance = true;

    private String store = JmqttConst.MEM;
    private Boolean useDefaultRdb = true;
    private Boolean useDefaultRedis = true;

    private AkkaConfig akka;
    private RDBConfig rdb;
    private RedisConfig redis;
    private NettyConfig netty;
    private Mqtt5Config mqtt5;

}
