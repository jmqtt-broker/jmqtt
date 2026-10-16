package com.jmqtt.core.mqtt.store.persist;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.jmqtt.core.mqtt.store.persist.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Slf4j
public class LocalDB {

    private final String localStoreDriver = "org.h2.Driver";
    private final String localStoreUrl = "jdbc:h2:file:/opt/jmqtt/jmqtt;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=1000";
    private final String localStoreUsername = "root";
    private final String localStorePassword = "123456";

    private static final LocalDB localStore;

    private DataSource dataSource;

    private LocalDB() {
    }

    private SqlSessionFactory sqlSessionFactory;

    private final AtomicBoolean start = new AtomicBoolean(false);

    static {
        localStore = new LocalDB();
    }

    public static LocalDB getInstance() {
        return localStore;
    }

    public static void startDB() {
        localStore.start();
    }

    public static void main(String[] args) {
        getInstance();
    }

    public void start() {
        if (this.start.compareAndSet(false, true)) {
            log.info("local store start...");
            this.dataSource = buildDataSource();

            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("development", transactionFactory, dataSource);

            MybatisConfiguration configuration = new MybatisConfiguration(environment);
            configuration.setMapUnderscoreToCamelCase(true);

            MybatisPlusInterceptor mpInterceptor = new MybatisPlusInterceptor();
            mpInterceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
            configuration.addInterceptor(mpInterceptor);

            registerMappers(configuration);

            this.sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);

            try {
                initSqlScript(dataSource);
            } catch (Exception e) {
                log.error("init sql error.", e);
                throw new RuntimeException("init sql error.");
            }
            log.info("local store start success...");
        }
    }

    private DataSource buildDataSource() {
        PooledDataSource dds = new PooledDataSource();
        dds.setDriver(localStoreDriver);
        dds.setUrl(localStoreUrl);
        dds.setUsername(localStoreUsername);
        dds.setPassword(localStorePassword);
        return dds;
    }

    private void registerMappers(MybatisConfiguration configuration) {
        configuration.addMapper(BrokerConfigMapper.class);
        configuration.addMapper(BrokerMapper.class);
        configuration.addMapper(OfflineMessageMapper.class);
        configuration.addMapper(RetainMessageMapper.class);
        configuration.addMapper(ScheduleTaskMapper.class);
        configuration.addMapper(SessionMapper.class);
        configuration.addMapper(SubscriptionMapper.class);
        configuration.addMapper(WillMessageMapper.class);
    }

    private void initSqlScript(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String initSql = "sql/jmqtt_local.sql";
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(initSql);
            if (is == null) {
                throw new RuntimeException("sql script not found!");
            }
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setLogWriter(null);
            runner.setAutoCommit(true);
            runner.runScript(new InputStreamReader(is));
            is.close();
        }
    }

    public <R> R operate(DBCallback<R> dbCallback) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return dbCallback.operate(sqlSession);
        }
    }

    public <T, R> R execute(Class<T> clazz, Function<T, R> func) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return func.apply(sqlSession.getMapper(clazz));
        }
    }

}
