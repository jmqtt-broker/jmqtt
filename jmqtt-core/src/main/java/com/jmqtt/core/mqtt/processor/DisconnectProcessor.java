package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.netty.helper.NettyUtil;
import com.jmqtt.core.netty.session.ClientSession;
import com.jmqtt.core.netty.session.ConnectManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端主动发起断开连接：正常断连
 */
@Slf4j
public class DisconnectProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        ctx.channel().attr(AttributeKey.valueOf("NORMAL_DISCONNECTION")).set(true);
        String clientId = NettyUtil.getClientId(ctx.channel());
        log.info("[DISCONNECT remote:{}] -> {} disconnect mqtt server", ctx.channel().remoteAddress(), clientId);
        ClientSession session = ConnectManager.getInstance().getClient(clientId);
        if (session != null) {
            MqttReasonCodeAndPropertiesVariableHeader header = (MqttReasonCodeAndPropertiesVariableHeader) mqttMessage.variableHeader();
            if (session.isMqtt5() && header.reasonCode() == 0x04) {
                // Disconnect with Will Message
                // 客户端主动正常断开连接，但希望服务端发布遗嘱消息，默认情况下只有异常断开连接时才发布遗嘱
                ctx.channel().attr(AttributeKey.valueOf("PUBLISH_WILL")).set(true);
            }
            ctx.close();
        } else {
            log.warn("[DISCONNECT] -> {} hasn't connect before", clientId);
        }
    }

}
