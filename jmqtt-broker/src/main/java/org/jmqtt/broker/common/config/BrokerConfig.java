package org.jmqtt.broker.common.config;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Getter;
import lombok.Setter;
import org.jmqtt.broker.common.JmqttConst;

import java.io.File;

@Getter
@Setter
public class BrokerConfig {

    // 配置conf文件的所在位置，logback，properties文件等所在位置
    private String jmqttHome = System.getenv("JMQTT_HOME") != null ? System.getenv("JMQTT_HOME") : System.getProperty("user.dir") +
            File.separator + "jmqtt-broker" + File.separator + "src" + File.separator + "main" + File.separator + "resources";
    private String logLevel = "INFO";

    private String  version         = "1.0.0";
    private boolean anonymousEnable = false;
    private String user = "admin";
    private String pwd = "admin";

    private int pollThreadNum = Runtime.getRuntime().availableProcessors() * 2;

    // 采用拉消息方式时，一次最多拉的消息数目
    private int maxPollEventNum  = 10;
    private int pollWaitInterval = 10;//ms

    private boolean akkaEnable = false;

    // mem、redis、mysql，默认mem
    private String store = JmqttConst.MEM;

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

    /* 本地存储相关配置 */
    private String localStoreDriver   = "org.h2.Driver";
    private String localStoreUrl = "jdbc:h2:file:~/jmqtt;AUTO_SERVER=true;MODE=MYSQL";
    private String localStoreUsername = "root";
    private String localStorePassword = "123456";

    /* processor support plugins */
    private String connectProcessorClass = "org.jmqtt.broker.processor.protocol.ConnectProcessor";
    private String disconnectProcessorClass = "org.jmqtt.broker.processor.protocol.DisconnectProcessor";
    private String publishProcessorClass = "org.jmqtt.broker.processor.protocol.PublishProcessor";

    private AkkaConfig akka;
    private RDBConfig rdb;
    private RedisConfig redis;

    // 是否使用服务端默认的心跳周期
    private boolean useServerKeepalive = false;
    private int defaultKeepalive = 60;
    // 服务端能同时处理的非qos0最大消息数
    private Integer receiveMaximum = 65535;
    // 服务端能处理的最大packet长度，默认256M，也是mqtt报文数据最大长度（可变头 + payload）
    private Integer maximumPacketSize = 1024 * 1024 * 256;
    // 主题别名最大值
    private Integer topicAliasMaximum = 65535;
    // 服务端生成的clientId默认前缀
    private String clientIdPrefix = "JMQTT_CLIENT_ID_";
    // 是否支持通配符订阅
    private Boolean wildcardSubscriptionAvailable = true;
    // 是否支持订阅标识符
    private Boolean subscriptionIdentifierAvailable = true;
    // 是否支持共享订阅
    private Boolean sharedSubscriptionAvailable = true;
    // 是否支持保留消息
    private Boolean retainAvailable = true;
    // 支持的最大QOS等级
    private Integer maximumQos = MqttQoS.EXACTLY_ONCE.value();

}
