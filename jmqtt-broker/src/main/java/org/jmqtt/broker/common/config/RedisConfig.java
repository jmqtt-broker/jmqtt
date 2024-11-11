package org.jmqtt.broker.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/10/31 14:34
 */
@Getter
@Setter
public class RedisConfig {

    private String redisHost = "127.0.0.1";
    private Integer redisPort = 6379;
    private String redisPassword;
    private Integer database = 0;
    private Integer maxWaitMills = 60 * 1000;
    private Integer minIdle = 20;
    private Integer maxIdle = 50;
    private Integer maxTotal = 200;

}
