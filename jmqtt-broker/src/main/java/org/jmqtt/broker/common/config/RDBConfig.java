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

    private String driver = "com.mysql.jdbc.Driver";
    private String url = "jdbc:mysql://localhost:3306/jmqtt?characterEncoding=utf8&autoReconnect=true&failOverReadOnly=false"
            + "&maxReconnects=10&useSSL=false";
    private String username = "root";
    private String password = "123456";

}
