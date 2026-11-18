package com.jmqtt.core.mqtt;

import com.jmqtt.core.JmqttBroker;
import com.jmqtt.core.config.JmqttConfig;
import com.jmqtt.core.exception.BrokerException;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.control.BaseCtrl;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.helper.Reason;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.NettyServer;
import com.jmqtt.core.netty.event.ChannelEventListener;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.HashMap;
import java.util.Map;

public class JmqttContext {

    private static JmqttBroker broker;

    private static String currentNodeName;

    private static final Map<MqttMessageType, BaseCtrl> ctrlTable = new HashMap<>();

    public static void setBroker(JmqttBroker broker) {
        if (JmqttContext.broker == null) {
            JmqttContext.broker = broker;
            currentNodeName = broker.getJmqttConfig().getCluster().getCurrentNodeName();
        }
    }

    public static void registerCtrl(MqttMessageType mqttType, BaseCtrl ctrl) {
        ctrlTable.put(mqttType, ctrl);
    }

    public static BaseCtrl getCtrl(MqttMessageType messageType) {
        BaseCtrl ctrl = ctrlTable.get(messageType);
        if (ctrl == null) {
            throw BrokerException.of(Reason.UNSUPPORTED_PROTOCOL_VERSION);
        }
        return ctrlTable.get(messageType);
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
