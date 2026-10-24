package com.jmqtt.core;

import cn.hutool.setting.yaml.YamlUtil;
import com.jmqtt.core.cluster.JmqttCluster;
import com.jmqtt.core.config.JmqttConfig;
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
import com.jmqtt.core.netty.NettyServer;
import com.jmqtt.core.netty.event.DefaultChannelEventListener;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

    public JmqttBroker() {
        this.jmqttConfig = YamlUtil.load(JmqttConfig.class.getClassLoader().getResourceAsStream("jmqtt.yml"), JmqttConfig.class);

        // 初始化订阅匹配器
        this.subscriptionMatcher = new DefaultSubscriptionTreeMatcher();
        // 初始化会话存储和消息存储
        this.sessionStore = new LocalSessionStore(this.subscriptionMatcher);
        this.messageStore = new LocalMessageStore();
        // 初始化认证器
        this.jmqttAuth = new DefaultJmqttAuth();
        // 初始化消息分发器
        this.messageDispatcher = new MessageDispatcher(jmqttConfig.getCluster(), jmqttConfig.getBroker(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher);
        // 启动netty服务
        this.channelEventListener = new DefaultChannelEventListener(this.sessionStore);
        this.nettyServer = new NettyServer(jmqttConfig.getNetty(), this.channelEventListener);
        // 注册mqtt控制指令报文处理器
        initCtrl();
        // 上下文工具
        JmqttContext.setBroker(this);
        // 启动集群，若配置未开启，则不启动
        JmqttCluster.start(this.jmqttConfig.getCluster());
    }

    private void initCtrl() {
        this.nettyServer.registerCtrl(MqttMessageType.CONNECT,
                new ConnectCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                        this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.DISCONNECT, new DisconnectCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.PINGREQ, new PingCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.PUBLISH, new PublishCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.PUBACK, new PubAckCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.PUBREC, new PubRecCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.PUBREL, new PubRelCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.PUBCOMP, new PubCompCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.SUBSCRIBE, new SubscribeCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
        this.nettyServer.registerCtrl(MqttMessageType.UNSUBSCRIBE, new UnSubscribeCtrl(this.jmqttAuth, this.jmqttConfig.getBroker(), this.jmqttConfig.getCluster(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher, this.messageDispatcher));
    }

    public static void start(String[] args) throws Exception {
        log.info("Jmqtt broker start begin.");
        JmqttBroker starter = new JmqttBroker();
        starter.startJmqtt();
    }

    private void startJmqtt() {
        LocalDB.startDB();
        this.nettyServer.start();
    }

    public void shutdown() {
        this.nettyServer.stop();
    }

    public static void main(String[] args) {
        try {
            start(args);
        } catch (Exception e) {
            log.error("Jmqtt start failed!", e);
        }
    }

}
