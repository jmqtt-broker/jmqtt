package org.jmqtt.starter.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.store.rdb.DBUtils;
import org.jmqtt.broker.store.redis.support.RedisUtils;
import org.jmqtt.starter.config.JmqttConfiguration;
import org.jmqtt.starter.redis.RedisOperatorImpl;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/11/4 16:23
 */
@Slf4j
@RequiredArgsConstructor
public class JmqttStartup {

    private final ApplicationContext ctx;

    private final BrokerConfig brokerConfig;

    private final JmqttConfiguration jmqttConfiguration;

    @PostConstruct
    public void start() {
        String store = brokerConfig.getStore();
        if (SessionStore.RDB.equals(store)) {
            initRdb();
        } else if (SessionStore.REDIS.equals(store)) {
            initRedis();
        }
        BrokerController ctrl = ctx.getBean(BrokerController.class);
        Optional.of(ctx.getBeansOfType(RequestProcessor.class)).ifPresent(ctrlMap ->
                ctrlMap.values().forEach(ctrl::addRequestProcessor));
        ctrl.start();
    }

    private void initRdb() {
        DataSource dataSource;
        try {
            if (jmqttConfiguration.getUseDefaultRdb()) {
                Map<String, DataSource> map = ctx.getBeansOfType(DataSource.class);
                dataSource = map.values().stream().findAny().orElseThrow(RuntimeException::new);
            } else {
                dataSource = (DataSource) ctx.getBean("jmqttDataSource");
            }
        } catch (BeansException e) {
            log.error("can not find datasource!", e);
            throw new RuntimeException("can not find datasource!");
        }
        DBUtils dbUtils = DBUtils.getInstance();
        dbUtils.start(brokerConfig, dataSource);
    }

    private void initRedis() {
        if (jmqttConfiguration.getUseDefaultRedis()) {
            try {
                Map<String, RedisConnectionFactory> factoryMap = ctx.getBeansOfType(RedisConnectionFactory.class);
                Map<String, RedisMessageListenerContainer> containerMap = ctx.getBeansOfType(RedisMessageListenerContainer.class);
                if (!factoryMap.isEmpty() && !containerMap.isEmpty()) {
                    RedisConnectionFactory factory = factoryMap.values().stream().findAny().get();
                    String hostname = "localhost";
                    int port = 6379;
                    int database = 0;
                    String password = null;
                    if (factory instanceof JedisConnectionFactory) {
                        JedisConnectionFactory jf = (JedisConnectionFactory) factory;
                        hostname = jf.getHostName();
                        port = jf.getPort();
                        database = jf.getDatabase();
                        password = jf.getPassword();
                    } else if (factory instanceof LettuceConnectionFactory) {
                        LettuceConnectionFactory lf = (LettuceConnectionFactory) factory;
                        hostname = lf.getHostName();
                        port = lf.getPort();
                        database = lf.getDatabase();
                        password = lf.getPassword();
                    }
                    if (!("localhost".equals(hostname) && port == 6379
                            && database == 0 && password == null)) {
                        // 依赖方Springboot环境配置了redis连接信息
                        RedisMessageListenerContainer container = containerMap.values().stream().findAny().get();
                        RedisUtils.getInstance().setOperator(new RedisOperatorImpl(factory, container));
                    }
                }
            } catch (BeansException e) {
                log.warn("redisConnectionFactory not found!");
            }
        }
    }

}
