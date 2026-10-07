package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.mqtt.helper.MessageUtil;
import com.jmqtt.core.netty.helper.NettyUtil;
import com.jmqtt.core.netty.helper.RemotingHelper;
import com.jmqtt.core.netty.session.ConnectManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端的pubRel报文：入栈消息qos2的第二阶段：
 * 1. 消息所有者转换为broker,开始分发消息
 * 2. 返回pubCom报文
 */
@Slf4j
public class PubRelProcessor extends AbstractMessageProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        String clientId = NettyUtil.getClientId(ctx.channel());
        int msgId = MessageUtil.getMessageId(mqttMessage);
        if(ConnectManager.getInstance().containClient(clientId)){
            // Message message = releaseInflowMsg(clientId,msgId);
            // if(Objects.nonNull(message)){
            //     super.processMessage(message);
            // }else{
            //     log.warn("[PubRelMessage] -> the message is not exist,clientId={},messageId={}.",clientId,msgId);
            // }
            // MqttMessage pubComMessage = MessageUtil.getPubReplyMessage(msgId, MqttMessageType.PUBCOMP, (byte) 0x00, null, false);
            // ctx.writeAndFlush(pubComMessage);
        }else{
            log.warn("[PubRelMessage] -> the client：{} disconnect to this server.",clientId);
            RemotingHelper.closeChannel(ctx.channel());
        }
    }
}
