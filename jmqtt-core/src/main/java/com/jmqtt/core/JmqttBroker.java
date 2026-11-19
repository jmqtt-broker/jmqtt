package com.jmqtt.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.jmqtt.core.cluster.JmqttCluster;
import com.jmqtt.core.config.JmqttConfig;
import com.jmqtt.core.mqtt.JmqttContext;
import com.jmqtt.core.mqtt.auth.DefaultJmqttAuth;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.control.*;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.store.LocalMessageStore;
import com.jmqtt.core.mqtt.store.LocalSessionStore;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.store.persist.LocalDB;
import com.jmqtt.core.mqtt.subscribe.DefaultSubscriptionTreeMatcher;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.mqtt.timer.TimerUtil;
import com.jmqtt.core.netty.NettyServer;
import com.jmqtt.core.netty.event.DefaultChannelEventListener;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.util.Arrays;

@Slf4j
@Getter
public class JmqttBroker {

    private final JmqttConfig jmqttConfig;

    private final NettyServer nettyServer;

    private final DefaultChannelEventListener channelEventListener;

    private final MessageDispatcher messageDispatcher;

    private final SubscriptionMatcher subscriptionMatcher;

    private final JmqttAuth jmqttAuth;

    private final SessionStore sessionStore;

    private final MessageStore messageStore;

    public JmqttBroker(String configPath) throws Exception {
        JmqttConfig config = null;
        if (StrUtil.isNotBlank(configPath)) {
            config = YamlUtil.load(new FileReader(configPath), JmqttConfig.class);
        }
        if (config == null) {
            config = YamlUtil.load(JmqttConfig.class.getClassLoader().getResourceAsStream("jmqtt.yml"), JmqttConfig.class);
        }
        this.jmqttConfig = config;
        // 初始化订阅匹配器
        this.subscriptionMatcher = new DefaultSubscriptionTreeMatcher();
        // 初始化会话存储和消息存储
        this.sessionStore = new LocalSessionStore(this.subscriptionMatcher);
        this.messageStore = new LocalMessageStore();
        // 初始化认证器
        this.jmqttAuth = new DefaultJmqttAuth();
        // 初始化消息分发器
        this.messageDispatcher = new MessageDispatcher(this.sessionStore, this.messageStore, this.subscriptionMatcher);
        // 初始化netty服务
        this.channelEventListener = new DefaultChannelEventListener(this.sessionStore, this.messageStore, this.messageDispatcher);
        this.nettyServer = new NettyServer(jmqttConfig.getNetty(), this.channelEventListener);
        // 注册mqtt控制指令报文处理器
        initCtrl();
        // 上下文工具
        JmqttContext.setBroker(this);
    }

    private void initCtrl() {
        JmqttContext.registerCtrl(MqttMessageType.CONNECT,
                new ConnectCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                        this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.DISCONNECT, new DisconnectCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.PINGREQ, new PingCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.PUBLISH, new PublishCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.PUBACK, new PubAckCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.PUBREC, new PubRecCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.PUBREL, new PubRelCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.PUBCOMP, new PubCompCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.SUBSCRIBE, new SubscribeCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        JmqttContext.registerCtrl(MqttMessageType.UNSUBSCRIBE, new UnSubscribeCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
    }

    public static void start(String... args) throws Exception {
        log.info("Jmqtt broker starting...");
        String configPath = Arrays.stream(args).filter(a -> a.startsWith("--config"))
                .findAny().map(s -> s.replace("--config=", "").trim()).orElse(null);
        JmqttBroker broker = new JmqttBroker(configPath);
        broker.startJmqtt();

    }

    private void startJmqtt() throws Exception {
        // 启动存储
        LocalDB.startDB();
        // 启动集群，若配置未开启，则不启动
        JmqttCluster.start(this.jmqttConfig.getCluster());
        // 调度任务管理
        TimerUtil.start();
        // 启动网络通信
        this.nettyServer.start();
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(this::shutdown));
    }

    public void shutdown() {
        this.nettyServer.stop();
        this.messageDispatcher.stop();
        TimerUtil.stop();
        JmqttCluster.shutdown();
        log.info("Jmqtt broker shutdown!");
    }

    public static void main(String[] args) {
        try {
            start(args);
        } catch (Exception e) {
            log.error("Jmqtt start failed!", e);
        }
    }

}
