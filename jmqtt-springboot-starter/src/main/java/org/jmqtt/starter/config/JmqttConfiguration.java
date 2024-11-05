package org.jmqtt.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.config.RDBConfig;
import org.jmqtt.broker.common.config.RedisConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/10/31 14:29
 */
@ConfigurationProperties(prefix = "jmqtt.broker")
@Configuration
@Getter
@Setter
public class JmqttConfiguration {

    private String store = "mem";

    private AkkaConfig akka;
    private RDBConfig rdb;
    private RedisConfig redis;
    private NettyConfig netty;

}
