package org.jmqtt.broker.processor.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.util.AttributeKey;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.NettyUtil;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.jmqtt.broker.subscribe.SubscriptionMatcher;
import org.slf4j.Logger;

/**
 * 客户端主动发起断开连接：正常断连
 */
public class DisconnectProcessor implements RequestProcessor {

    private static final Logger log = JmqttLogger.clientTraceLog;
    private SessionStore sessionStore;
    private MessageStore messageStore;
    private SubscriptionMatcher subscriptionMatcher;

    public DisconnectProcessor(BrokerController brokerController) {
        this.sessionStore = brokerController.getSessionStore();
        this.messageStore = brokerController.getMessageStore();
        this.subscriptionMatcher = brokerController.getSubscriptionMatcher();
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        ctx.channel().attr(AttributeKey.valueOf("NORMAL_DISCONNECTION")).set(true);
        String clientId = NettyUtil.getClientId(ctx.channel());
        LogUtil.info(log, "[DISCONNECT remote:{}] -> {} disconnect mqtt server", ctx.channel().remoteAddress(), clientId);
        ClientSession session = ConnectManager.getInstance().getClient(clientId);
        if (session != null) {
            MqttReasonCodeAndPropertiesVariableHeader header = (MqttReasonCodeAndPropertiesVariableHeader) mqttMessage.variableHeader();
            if (session.isMqtt5() && header.reasonCode() == 0x04) {
                // Disconnect with Will Message
                //客户端主动正常断开连接，但希望服务端发布遗嘱消息，默认情况下只有异常断开连接时才发布遗嘱
                ctx.channel().attr(AttributeKey.valueOf("PUBLISH_WILL")).set(true);
            }
            ctx.close();
        } else {
            LogUtil.warn(log, "[DISCONNECT] -> {} hasn't connect before", clientId);
        }
    }

}
