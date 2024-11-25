package org.jmqtt.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.config.RDBConfig;
import org.jmqtt.broker.common.config.RedisConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/10/31 14:29
 */
@ConfigurationProperties(prefix = "jmqtt.broker")
@Getter
@Setter
public class JmqttConfiguration {

    private Boolean anonymousEnable = false;
    private String user = "admin";
    private String pwd = "admin";
    private Boolean highPerformance = false;

    private String store = "mem";
    private Boolean useDefaultRdb = true;
    private Boolean useDefaultRedis = true;

    private AkkaConfig akka;
    private RDBConfig rdb;
    private RedisConfig redis;
    private NettyConfig netty;

}
