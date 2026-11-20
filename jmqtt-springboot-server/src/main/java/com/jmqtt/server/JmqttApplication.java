package com.jmqtt.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
@MapperScan(basePackages = "com.jmqtt.server.mapper")
public class JmqttApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmqttApplication.class, args);
    }

}
