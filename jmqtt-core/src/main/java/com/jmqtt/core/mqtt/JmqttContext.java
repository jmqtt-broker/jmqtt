package com.jmqtt.core.mqtt;

import com.jmqtt.core.JmqttBroker;
import com.jmqtt.core.config.JmqttConfig;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.NettyServer;
import com.jmqtt.core.netty.event.ChannelEventListener;

public class JmqttContext {

    private static JmqttBroker broker;

    private static String currentNodeName;

    public static void setBroker(JmqttBroker broker) {
        JmqttContext.broker = broker;
        currentNodeName = broker.getJmqttConfig().getCluster().getCurrentNodeName();
    }

    public static boolean isLocal(String nodeName) {
        return currentNodeName.equals(nodeName);
    }

    public static String currentNodeName() {
        return currentNodeName;
    }

    public static JmqttConfig getJmqttConfig() {
        return broker.getJmqttConfig();
    }

    public static JmqttAuth getJmqttAuth() {
        return broker.getJmqttAuth();
    }

    public static SessionStore getSessionStore() {
        return broker.getSessionStore();
    }

    public static MessageStore getMessageStore() {
        return broker.getMessageStore();
    }

    public static SubscriptionMatcher getSubscriptionMatcher() {
        return broker.getSubscriptionMatcher();
    }

    public static MessageDispatcher getMessageDispatcher() {
        return broker.getMessageDispatcher();
    }

    public static NettyServer getNettyServer() {
        return broker.getNettyServer();
    }

    public static ChannelEventListener getChannelEventListener() {
        return broker.getChannelEventListener();
    }

}
