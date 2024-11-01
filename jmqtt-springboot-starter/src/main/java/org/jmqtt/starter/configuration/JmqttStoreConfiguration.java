package org.jmqtt.starter.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.mem.MemMessageStore;
import org.jmqtt.broker.store.mem.MemSessionStore;
import org.jmqtt.broker.store.rdb.RDBMessageStore;
import org.jmqtt.broker.store.rdb.RDBSessionStore;
import org.jmqtt.broker.store.redis.RedisMessageStore;
import org.jmqtt.broker.store.redis.RedisSessionStore;
import org.jmqtt.starter.config.JmqttConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/11/1 15:37
 */
@Configuration
@Slf4j
public class JmqttStoreConfiguration {

    @Autowired
    private ApplicationContext ctx;

    @Bean
    public SessionStore sessionStore(BrokerController brokerController, JmqttConfiguration autoConfig) {
        String store = autoConfig.getStore();
        SessionStore sessionStore;
        if (SessionStore.MYSQL.equals(store)) {
            sessionStore = new RDBSessionStore();
        } else if (SessionStore.REDIS.equals(store)) {
            sessionStore = new RedisSessionStore();
        } else {
            sessionStore = new MemSessionStore();
        }
        brokerController.setSessionStore(sessionStore);
        return sessionStore;
    }

    @Bean
    public MessageStore messageStore(BrokerController brokerController, JmqttConfiguration autoConfig) {
        String store = autoConfig.getStore();
        MessageStore messageStore;
        if (SessionStore.MYSQL.equals(store)) {
            messageStore = new RDBMessageStore();
        } else if (SessionStore.REDIS.equals(store)) {
            messageStore = new RedisMessageStore();
        } else {
            messageStore = new MemMessageStore();
        }
        brokerController.setMessageStore(messageStore);
        return messageStore;
    }

    @PostConstruct
    public void start() {
        ctx.getBean(BrokerController.class).start();
    }

}
