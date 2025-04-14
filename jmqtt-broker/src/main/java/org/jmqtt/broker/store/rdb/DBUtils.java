package org.jmqtt.broker.store.rdb;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.store.rdb.mapper.*;
import org.slf4j.Logger;
import tk.mybatis.mapper.mapperhelper.MapperHelper;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * db 工具类
 */
public class DBUtils {

    private static final Logger log = JmqttLogger.storeLog;

    private static final DBUtils dbUtils = new DBUtils();

    private DataSource dataSource;

    private DBUtils() {
    }

    private SqlSessionFactory sqlSessionFactory;

    private AtomicBoolean start = new AtomicBoolean(false);

    public static DBUtils getInstance() {
        return dbUtils;
    }

    public void start(BrokerConfig brokerConfig) {
        start(brokerConfig, null);
    }

    public void start(BrokerConfig brokerConfig, DataSource dataSource) {
        if (this.start.compareAndSet(false, true)) {
            LogUtil.info(log, "DB store start...");
            if (dataSource == null) {
                dataSource = new DataSourceFactory() {
                    @Override
                    public void setProperties(Properties properties) {
                    }

                    @Override
                    public DataSource getDataSource() {
                        DruidDataSource dds = new DruidDataSource();
                        dds.setDriverClassName(brokerConfig.getDriver());
                        dds.setUrl(brokerConfig.getUrl());
                        dds.setUsername(brokerConfig.getUsername());
                        dds.setPassword(brokerConfig.getPassword());
                        // 其他配置可自行补充
                        dds.setKeepAlive(true);
                        dds.setMinEvictableIdleTimeMillis(180000);
                        dds.setMaxWait(60000);
                        dds.setInitialSize(5);
                        dds.setMinIdle(5);
                        try {
                            dds.init();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                        return dds;
                    }
                }.getDataSource();
            }
            this.dataSource = dataSource;
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("development", transactionFactory, dataSource);
            Configuration configuration = new Configuration(environment);
            // 全局变量需要写在addMapper之前，否则不生效
            Properties props = new Properties();
            props.setProperty("dbType", getDbType(dataSource));
            configuration.setVariables(props);
            // 初始化所有mapper
            configuration.addMapper(SessionMapper.class);
            configuration.addMapper(SubscriptionMapper.class);
            configuration.addMapper(OfflineMessageMapper.class);
            configuration.addMapper(EventMapper.class);
            configuration.addMapper(InflowMessageMapper.class);
            configuration.addMapper(OutflowSecMessageMapper.class);
            configuration.addMapper(RetainMessageMapper.class);
            configuration.addMapper(OutflowMessageMapper.class);
            configuration.addMapper(WillMessageMapper.class);
            configuration.setMapUnderscoreToCamelCase(true);
            PageInterceptor pageInterceptor = new PageInterceptor();
            Properties properties = new Properties();
            properties.setProperty("reasonable", "true");
            properties.setProperty("supportMethodsArguments", "true");
            properties.setProperty("autoRuntimeDialect", "true");
            pageInterceptor.setProperties(properties);
            configuration.addInterceptor(pageInterceptor);
            MapperHelper mapperHelper = new MapperHelper();
            mapperHelper.processConfiguration(configuration);
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
            LogUtil.info(log, "DB store start success...");
            try {
                initSqlScript(dataSource);
            } catch (Exception e) {
                log.error("init sql error.", e);
                throw new RuntimeException("init sql error.");
            }
        }
    }

    private String getDbType(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductName().toLowerCase();
        } catch (Exception e) {
            LogUtil.error(log, "get dbType faild", e);
            throw new RuntimeException("get dbType faild");
        }
    }

    private void initSqlScript(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName();
            String initSql;
            if ("MySQL".equalsIgnoreCase(dbName)) {
                initSql = "conf/jmqtt_mysql.sql";
            } else if ("PostgreSQL".equalsIgnoreCase(dbName)) {
                initSql = "conf/jmqtt_pgsql.sql";
            } else if ("Oracle".equalsIgnoreCase(dbName)) {
                initSql = "conf/jmqtt_oracle.sql";
            } else {
                throw new RuntimeException("unsupport db type: " + dbName);
            }
            log.info("数据库类型：{}，初始化数据库脚本：{}", dbName, initSql);
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(initSql);
            if (is == null) {
                throw new RuntimeException("sql script not found!");
            }
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setAutoCommit(true);
            runner.runScript(new InputStreamReader(is));
            is.close();
            log.info("sql script init.");
        }
    }

    public void shutdown() {
    }

    public <R> R operate(DBCallback<R> dbCallback) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return dbCallback.operate(sqlSession);
        }
    }

    /**
     * 获取关闭事物的session，需要手动提交事物
     *
     * @return return
     */
    public SqlSession getSqlSessionWithTrans() {
        return this.sqlSessionFactory.openSession(false);
    }

    public <T, R> R execute(Class<T> clazz, Function<T, R> func) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return func.apply(sqlSession.getMapper(clazz));
        }
    }
}
