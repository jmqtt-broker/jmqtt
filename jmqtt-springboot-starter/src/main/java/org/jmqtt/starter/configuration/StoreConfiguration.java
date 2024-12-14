package org.jmqtt.starter.configuration;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/11/11 9:58
 */
@AutoConfigureAfter({RedisAutoConfiguration.class, DataSourceAutoConfiguration.class})
@Slf4j
public class StoreConfiguration {

    @Bean("wrapDataSource")
    @ConditionalOnProperty(prefix = "jmqtt.broker", value = "store", havingValue = "rdb")
    public DataSource jmqttDataSource(BrokerConfig brokerConfig) {
        DruidDataSource dds = new DruidDataSource();
        dds.setDriverClassName(brokerConfig.getDriver());
        dds.setUrl(brokerConfig.getUrl());
        dds.setUsername(brokerConfig.getUsername());
        dds.setPassword(brokerConfig.getPassword());
        dds.setKeepAlive(true);
        dds.setMinEvictableIdleTimeMillis(180000);
        dds.setMaxWait(60000);
        dds.setInitialSize(5);
        dds.setMinIdle(5);
        try {
            dds.init();
            return dds;
        } catch (SQLException e) {
            log.error("init dataSource error.", e);
            throw new RuntimeException("init dataSource error.");
        }
    }

    @Bean("jmqttDataSource")
    @ConditionalOnBean(name = "wrapDataSource")
    @ConditionalOnProperty(prefix = "jmqtt.broker", value = "useDefaultRdb", havingValue = "false")
    public DataSource jmqttDataSource(@Qualifier("wrapDataSource") DataSource dataSource) {
        return dataSource;
    }

    @Bean("jmqttRedisMessageListenerContainer")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    @ConditionalOnProperty(prefix = "jmqtt.broker", value = "useDefaultRedis", havingValue = "true")
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        return container;
    }

}
