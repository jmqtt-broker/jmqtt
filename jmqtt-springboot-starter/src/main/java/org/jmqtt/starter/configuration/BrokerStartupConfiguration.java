package org.jmqtt.starter.configuration;

import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.remoting.netty.NettySslHandler;
import org.jmqtt.starter.config.JmqttConfiguration;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

@Configuration
public class BrokerStartupConfiguration {

    private static final Logger log = JmqttLogger.brokerlog;

    @Autowired
    private Environment environment;

    @Bean
    public Properties jmqttConfig() {
        Properties properties = new Properties();
        try (InputStream is = NettySslHandler.class.getClassLoader()
                .getResourceAsStream("conf/jmqtt.properties")) {
            properties.load(is);
        } catch (FileNotFoundException e) {
            log.error("jmqtt.properties cannot find.", e);
        } catch (IOException e) {
            log.error("Handle jmqttConfig IO exception.", e);
        }
        return properties;
    }

    @Bean
    public BrokerConfig brokerConfig(@Qualifier("jmqttConfig") Properties jmqttConfig, JmqttConfiguration autoConfig) {
        BrokerConfig brokerConfig = new BrokerConfig();
        MixAll.properties2POJO(jmqttConfig, brokerConfig);
        // 目的是去除值为null的key
        JSONObject jsonConfig = JSONObject.parseObject(JSONObject.toJSONString(autoConfig));
        BeanUtils.copyProperties(jsonConfig, brokerConfig);
        // getProperties(brokerConfig, "jmqtt.broker.");
        return brokerConfig;
    }

    @Bean
    public NettyConfig nettyConfig(@Qualifier("jmqttConfig") Properties jmqttConfig, JmqttConfiguration autoConfig) {
        NettyConfig nettyConfig = new NettyConfig();
        MixAll.properties2POJO(jmqttConfig, nettyConfig);
        BeanUtils.copyProperties(autoConfig.getNetty(), nettyConfig);
        // getProperties(nettyConfig, "jmqtt.broker.");
        return nettyConfig;
    }

    @Bean
    public BrokerController brokerController(BrokerConfig brokerConfig,
                                             NettyConfig nettyConfig) {
        BrokerController brokerController = new BrokerController(brokerConfig, nettyConfig);
        brokerController.start();
        Runtime.getRuntime().addShutdownHook(new Thread(brokerController::shutdown));
        return brokerController;
    }

    private void getProperties(Object object, String prefix) {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            String key = prefix + field.getName();
            if (environment.containsProperty(key)) {
                Field tempField = null;
                try {
                    tempField = object.getClass().getDeclaredField(field.getName());
                    tempField.setAccessible(true);
                    tempField.set(object, environment.getProperty(key, field.getType()));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}
