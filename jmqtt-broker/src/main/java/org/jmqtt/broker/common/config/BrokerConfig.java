package org.jmqtt.broker.common.config;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
public class BrokerConfig {

    // 配置conf文件的所在位置，logback，properties文件等所在位置
    private String jmqttHome = System.getenv("JMQTT_HOME") != null ? System.getenv("JMQTT_HOME") : System.getProperty("user.dir") +
            File.separator + "jmqtt-broker" + File.separator + "src" + File.separator + "main" + File.separator + "resources";
    private String logLevel = "INFO";

    private String  version         = "3.0.0";
    private boolean anonymousEnable = false;
    private String user = "admin";
    private String pwd = "admin";

    private int pollThreadNum = Runtime.getRuntime().availableProcessors() * 2;

    // 采用拉消息方式时，一次最多拉的消息数目
    private int maxPollEventNum  = 10;
    private int pollWaitInterval = 10;//ms

    private boolean akkaEnable = false;

    // mem、redis、mysql，默认mem
    private String store = "mem";

    // plugin class config
    // private String sessionStoreClass        = "org.jmqtt.broker.store.rdb.RDBSessionStore";
    private String sessionStoreClass        = "org.jmqtt.broker.store.mem.MemSessionStore";
    private String messageStoreClass        = "org.jmqtt.broker.store.mem.MemMessageStore";
    private String authValidClass           = "org.jmqtt.broker.acl.impl.DefaultAuthValid";
    // private String clusterEventHandlerClass = "org.jmqtt.broker.processor.dispatcher.rdb.RDBClusterEventHandler";
    private String clusterEventHandlerClass = "org.jmqtt.broker.processor.dispatcher.mem.MemEventHandler";
    private String channelEventListener = "org.jmqtt.broker.client.ClientLifeCycleHookService";

    /* redis相关配置 */
    private String  redisHost     = "127.0.0.1";
    private int     redisPort     = 6379;
    private String  redisPassword = "";
    private int     maxWaitMills  = 60 * 1000;
    private boolean testOnBorrow  = true;
    private int     database       = 0;
    private int     minIdle       = 20;
    private int     maxTotal      = 200;
    private int     maxIdle       = 50;

    /* db相关配置 */
    private String driver   = "com.mysql.jdbc.Driver";
    private String url
                            = "jdbc:mysql://localhost:3306/jmqtt?characterEncoding=utf8&autoReconnect=true&failOverReadOnly=false"
            + "&maxReconnects=10&useSSL=false";
    private String username = "root";
    private String password = "123456";

    // 是否启用高性能模式，高性能模式下：入栈消息，出栈消息等过程消息都会默认采用内存缓存，若为false，则会用具体实现的存储缓存这一阶段的消息
    private boolean highPerformance = true;

    /* processor support plugins */
    private String connectProcessorClass = "org.jmqtt.broker.processor.protocol.ConnectProcessor";
    private String disconnectProcessorClass = "org.jmqtt.broker.processor.protocol.DisconnectProcessor";
    private String publishProcessorClass = "org.jmqtt.broker.processor.protocol.PublishProcessor";

    private AkkaConfig akka;
    private RDBConfig rdb;
    private RedisConfig redis;

}
