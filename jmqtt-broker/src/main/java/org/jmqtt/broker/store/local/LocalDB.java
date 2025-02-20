package org.jmqtt.broker.store.local;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.store.local.mapper.*;
import org.jmqtt.broker.store.rdb.DBCallback;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalDB {

    private static final Logger log = JmqttLogger.storeLog;

    private String localStoreDriver = "org.h2.Driver";
    private String localStoreUrl = "jdbc:h2:file:~/jmqtt;AUTO_SERVER=true;MODE=MYSQL";
    private String localStoreUsername = "root";
    private String localStorePassword = "123456";

    private static final LocalDB localStore = new LocalDB();

    private DataSource dataSource;

    private LocalDB() {
    }

    private SqlSessionFactory sqlSessionFactory;

    private AtomicBoolean start = new AtomicBoolean(false);

    public static LocalDB getInstance() {
        return localStore;
    }

    public void start() {
        if (this.start.compareAndSet(false, true)) {
            LogUtil.info(log, "local store start...");
            this.dataSource = new DataSourceFactory() {
                @Override
                public void setProperties(Properties properties) {
                }

                @Override
                public DataSource getDataSource() {
                    DruidDataSource dds = new DruidDataSource();
                    dds.setDriverClassName(localStoreDriver);
                    dds.setUrl(localStoreUrl);
                    dds.setUsername(localStoreUsername);
                    dds.setPassword(localStorePassword);
                    dds.setKeepAlive(true);
                    dds.setMinEvictableIdleTimeMillis(180000);
                    dds.setMaxWait(60000);
                    dds.setInitialSize(5);
                    dds.setMinIdle(5);
                    dds.setTestWhileIdle(true);
                    dds.setValidationQuery("select 1");
                    try {
                        dds.init();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    return dds;
                }
            }.getDataSource();
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("development", transactionFactory, dataSource);
            Configuration configuration = new Configuration(environment);
            configuration.addMapper(LocalRetainMessageMapper.class);
            configuration.addMapper(LocalWillMessageMapper.class);
            configuration.addMapper(LocalOfflineMessageMapper.class);
            configuration.addMapper(LocalSubscriptionMapper.class);
            configuration.addMapper(LocalSessionMapper.class);
            configuration.addMapper(LocalBrokerMapper.class);
            configuration.setMapUnderscoreToCamelCase(true);
            this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
            LogUtil.info(log, "localStore store start success...");
            try {
                initSqlScript(dataSource);
            } catch (Exception e) {
                log.error("init sql error.", e);
                throw new RuntimeException("init sql error.");
            }
        }
    }

    private void initSqlScript(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String initSql = "conf/jmqtt_local.sql";
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(initSql);
            if (is == null) {
                throw new RuntimeException("sql script not found!");
            }
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setAutoCommit(true);
            runner.runScript(new InputStreamReader(is));
            is.close();
        }
    }

    public void shutdown() {
    }

    public Object operate(DBCallback dbCallback) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return dbCallback.operate(sqlSession);
        }
    }

}
