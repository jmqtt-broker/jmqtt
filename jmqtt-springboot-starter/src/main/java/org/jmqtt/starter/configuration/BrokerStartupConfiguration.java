package org.jmqtt.starter.configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.acl.impl.DefaultAuthValid;
import org.jmqtt.broker.client.ClientLifeCycleHookService;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.DefaultDispatcherInnerMessage;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.processor.dispatcher.akka.AkkaClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.mem.MemEventHandler;
import org.jmqtt.broker.processor.dispatcher.rdb.RDBClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.redis.RedisClusterEventHandler;
import org.jmqtt.broker.remoting.netty.ChannelEventListener;
import org.jmqtt.broker.remoting.netty.NettySslHandler;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.mem.MemMessageStore;
import org.jmqtt.broker.store.mem.MemSessionStore;
import org.jmqtt.broker.store.rdb.RDBMessageStore;
import org.jmqtt.broker.store.rdb.RDBSessionStore;
import org.jmqtt.broker.store.redis.RedisMessageStore;
import org.jmqtt.broker.store.redis.RedisSessionStore;
import org.jmqtt.broker.subscribe.DefaultSubscriptionTreeMatcher;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.jmqtt.starter.config.JmqttConfiguration;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class BrokerStartupConfiguration {

    private static final Logger log = JmqttLogger.brokerlog;

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
        JmqttConfiguration filterNull = JSONObject.parseObject(JSON.toJSONString(autoConfig), JmqttConfiguration.class);
        BeanUtils.copyProperties(filterNull, brokerConfig);
        Optional.ofNullable(autoConfig.getRdb()).ifPresent(rdb -> BeanUtils.copyProperties(rdb, brokerConfig));
        Optional.ofNullable(autoConfig.getRedis()).ifPresent(redis -> BeanUtils.copyProperties(redis, brokerConfig));
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
    public SessionStore sessionStore(JmqttConfiguration autoConfig) {
        String store = autoConfig.getStore();
        SessionStore sessionStore;
        if (SessionStore.RDB.equals(store)) {
            sessionStore = new RDBSessionStore();
        } else if (SessionStore.REDIS.equals(store)) {
            sessionStore = new RedisSessionStore();
        } else {
            sessionStore = new MemSessionStore();
        }
        return sessionStore;
    }

    @Bean
    public MessageStore messageStore(JmqttConfiguration autoConfig) {
        String store = autoConfig.getStore();
        MessageStore messageStore;
        if (SessionStore.RDB.equals(store)) {
            messageStore = new RDBMessageStore();
        } else if (SessionStore.REDIS.equals(store)) {
            messageStore = new RedisMessageStore();
        } else {
            messageStore = new MemMessageStore();
        }
        return messageStore;
    }

    @Bean
    public ClusterEventHandler clusterEventHandler(JmqttConfiguration autoConfig) {
        String store = autoConfig.getStore();
        ClusterEventHandler clusterEventHandler;
        if (autoConfig.getAkka().getEnable()) {
            clusterEventHandler = new AkkaClusterEventHandler();
        } else if (SessionStore.RDB.equals(store)) {
            clusterEventHandler = new RDBClusterEventHandler();
        } else if (SessionStore.REDIS.equals(store)) {
            clusterEventHandler = new RedisClusterEventHandler();
        } else {
            clusterEventHandler = new MemEventHandler();
        }
        return clusterEventHandler;
    }

    @Bean
    public SubscriptionMatcher subscriptionMatcher() {
        return new DefaultSubscriptionTreeMatcher();
    }

    @Bean
    public InnerMessageDispatcher innerMessageDispatcher(BrokerConfig brokerConfig, SessionStore sessionStore,
                                                         SubscriptionMatcher subscriptionMatcher, ClusterEventHandler clusterEventHandler) {
        return new DefaultDispatcherInnerMessage(brokerConfig.isHighPerformance(),
                sessionStore, brokerConfig.getPollThreadNum(), subscriptionMatcher, clusterEventHandler);
    }

    @Bean
    @ConditionalOnMissingBean(AuthValid.class)
    public AuthValid authValid() {
        return new DefaultAuthValid();
    }

    @Bean
    @ConditionalOnMissingBean(ChannelEventListener.class)
    public ChannelEventListener clientLifeCycleHookService(SessionStore sessionStore,
                                                           MessageStore messageStore,
                                                           SubscriptionMatcher subscriptionMatcher,
                                                           InnerMessageDispatcher innerMessageDispatcher) {
        return new ClientLifeCycleHookService(sessionStore, messageStore, subscriptionMatcher, innerMessageDispatcher);
    }

    @Bean
    public BrokerController brokerController(BrokerConfig brokerConfig,
                                             NettyConfig nettyConfig,
                                             SessionStore sessionStore,
                                             MessageStore messageStore,
                                             SubscriptionMatcher subscriptionMatcher,
                                             ClusterEventHandler clusterEventHandler,
                                             InnerMessageDispatcher innerMessageDispatcher,
                                             ChannelEventListener channelEventListener,
                                             AuthValid authValid) {
        return new BrokerController(brokerConfig, nettyConfig,
                sessionStore, messageStore, subscriptionMatcher, clusterEventHandler,
                innerMessageDispatcher, channelEventListener, authValid);
    }

}
