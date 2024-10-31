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

    private String redisHost;
    private Integer redisPort;
    private String redisPassword;
    private Integer maxWaitMills;
    private Integer minIdle;
    private Integer maxTotal;
    private Integer maxIdle;

}
