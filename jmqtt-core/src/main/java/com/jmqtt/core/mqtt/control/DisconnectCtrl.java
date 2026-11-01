package com.jmqtt.core.mqtt.control;

import com.jmqtt.core.cluster.ClusterConfig;
import com.jmqtt.core.cluster.ClusterMessage;
import com.jmqtt.core.cluster.JmqttCluster;
import com.jmqtt.core.cluster.record.ClusterSessionRecord;
import com.jmqtt.core.mqtt.auth.JmqttAuth;
import com.jmqtt.core.mqtt.config.BrokerConfig;
import com.jmqtt.core.mqtt.control.dispatcher.MessageDispatcher;
import com.jmqtt.core.mqtt.store.MessageStore;
import com.jmqtt.core.mqtt.store.SessionStore;
import com.jmqtt.core.mqtt.subscribe.SubscriptionMatcher;
import com.jmqtt.core.netty.helper.ChannelUtil;
import com.jmqtt.core.netty.session.ClientSession;
import com.jmqtt.core.netty.session.ClientManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端主动发起断开连接：正常断连
 */
@Slf4j
public class DisconnectCtrl extends BaseCtrl {

    public DisconnectCtrl(JmqttAuth jmqttAuth, BrokerConfig brokerConfig, ClusterConfig clusterConfig,
                          SessionStore sessionStore, MessageStore messageStore,
                          SubscriptionMatcher subscriptionMatcher, MessageDispatcher messageDispatcher) {
        super(jmqttAuth, brokerConfig, clusterConfig, sessionStore, messageStore, subscriptionMatcher, messageDispatcher);
    }

    @Override
    public void processCtrl(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        ctx.channel().attr(AttributeKey.valueOf("NORMAL_DISCONNECTION")).set(true);
        String clientId = ChannelUtil.getClientId(ctx.channel());
        ClientSession session = ClientManager.getClient(clientId);
        if (session != null) {
            MqttReasonCodeAndPropertiesVariableHeader header = (MqttReasonCodeAndPropertiesVariableHeader) mqttMessage.variableHeader();
            if (session.isMqtt5() && header.reasonCode() == 0x04) {
                // Disconnect with Will Message
                // 客户端主动正常断开连接，但希望服务端发布遗嘱消息，默认情况下只有异常断开连接时才发布遗嘱
                ctx.channel().attr(AttributeKey.valueOf("PUBLISH_WILL")).set(true);
            }
            ctx.close().addListener(future -> {
                if (future.isSuccess()) {
                    log.info("[DISCONNECT] -> {} disconnect mqtt server", clientId);
                    // 同步会话到其他节点
                    JmqttCluster.broadcast(ClusterMessage.of(ClusterMessage.EventType.SESSION_DISCONNECT, ClusterSessionRecord.of(clientId, session.isCleanStart())));
                }
            });
        } else {
            log.warn("[DISCONNECT] -> {} hasn't connect before", clientId);
        }
    }

}
