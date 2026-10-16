package com.jmqtt.core;

import cn.hutool.setting.yaml.YamlUtil;
import com.jmqtt.core.config.JmqttConfig;
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
import com.jmqtt.core.netty.event.ChannelEventListener;
import com.jmqtt.core.netty.event.DefaultChannelEventListener;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class JmqttStartup {

    private final JmqttConfig jmqttConfig;

    private final NettyServer nettyServer;

    private final ChannelEventListener channelEventListener;

    private final MessageDispatcher messageDispatcher;

    private final SubscriptionMatcher subscriptionMatcher;

    private final JmqttAuth jmqttAuth;

    private final SessionStore sessionStore;

    private final MessageStore messageStore;

    public JmqttStartup() {
        this.jmqttConfig = YamlUtil.load(JmqttConfig.class.getClassLoader().getResourceAsStream("jmqtt.yml"), JmqttConfig.class);
        this.channelEventListener = new DefaultChannelEventListener();
        this.nettyServer = new NettyServer(jmqttConfig.getNetty(), this.channelEventListener);

        this.sessionStore = new LocalSessionStore();
        this.messageStore = new LocalMessageStore();
        this.subscriptionMatcher = new DefaultSubscriptionTreeMatcher();
        this.jmqttAuth = new JmqttAuth() {
        };
        this.messageDispatcher = new MessageDispatcher(jmqttConfig.getCluster(), jmqttConfig.getBroker(),
                this.sessionStore, this.messageStore, this.subscriptionMatcher);
        initCtrl();
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
        log.info("Jmqtt start begin.");
        JmqttStartup starter = new JmqttStartup();
        starter.startJmqtt();
    }

    private void startJmqtt() {
        LocalDB.startDB();
        this.nettyServer.start();
    }

    public static void main(String[] args) {
        try {
            start(args);
        } catch (Exception e) {
            log.error("Jmqtt start failed!", e);
        }
    }

}
