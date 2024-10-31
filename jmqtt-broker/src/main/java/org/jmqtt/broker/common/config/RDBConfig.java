package org.jmqtt.broker.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/10/31 14:33
 */
@Getter
@Setter
public class RDBConfig {

    private String driver;
    private String url;
    private String username;
    private String password;

}
