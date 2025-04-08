package org.jmqtt.starter.configuration;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.store.local.LocalDB;
import org.jmqtt.broker.store.rdb.DBUtils;
import org.jmqtt.broker.store.redis.support.RedisUtils;
import org.jmqtt.common.config.JmqttConst;
import org.jmqtt.starter.config.JmqttConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

@AutoConfigureAfter({RedisAutoConfiguration.class, DataSourceAutoConfiguration.class})
@Slf4j
public class StoreConfiguration {

    @Bean("localDb")
    public LocalDB localDb() {
        LocalDB.getInstance().start();
        return LocalDB.getInstance();
    }

    @Bean("dbUtils")
    @ConditionalOnProperty(prefix = "jmqtt.broker", value = "store", havingValue = JmqttConst.RDB)
    @ConditionalOnBean(DataSource.class)
    public DBUtils dbUtils(BrokerConfig brokerConfig,
                           JmqttConfiguration jmqttConfiguration,
                           ApplicationContext ctx) {
        DataSource dataSource;
        if (jmqttConfiguration.getUseDefaultRdb()) {
            Map<String, DataSource> map = ctx.getBeansOfType(DataSource.class);
            dataSource = map.values().stream().findAny().orElseThrow(RuntimeException::new);
        } else {
            dataSource = (DataSource) ctx.getBean("jmqttDataSource");
        }
        DBUtils.getInstance().start(brokerConfig, dataSource);
        return DBUtils.getInstance();
    }

    @Bean
    @ConditionalOnProperty(prefix = "jmqtt.broker", value = "store", havingValue = JmqttConst.REDIS)
    public RedisUtils redisUtils() {
        return RedisUtils.getInstance();
    }

    @Bean("wrapDataSource")
    @ConditionalOnProperty(prefix = "jmqtt.broker", value = "store", havingValue = JmqttConst.RDB)
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
