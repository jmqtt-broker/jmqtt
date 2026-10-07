package com.jmqtt.core.mqtt.processor;

import com.jmqtt.core.netty.helper.RemotingHelper;
import com.jmqtt.core.netty.session.ClientSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectProcessor implements RequestProcessor {

    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttConnectMessage connectMessage = (MqttConnectMessage) mqttMessage;
        MqttConnectReturnCode returnCode = MqttConnectReturnCode.CONNECTION_ACCEPTED;
        MqttConnectVariableHeader variableHeader = connectMessage.variableHeader();
        int mqttVersion = variableHeader.version();
        boolean mqtt5 = mqttVersion == MqttVersion.MQTT_5.protocolLevel();
        String clientId = connectMessage.payload().clientIdentifier();
        boolean cleanSession = variableHeader.isCleanSession();
        String userName = connectMessage.payload().userName();
        byte[] password = connectMessage.payload().passwordInBytes();
        ClientSession clientSession;
        boolean sessionPresent = false;
        String remoteAddress = RemotingHelper.getRemoteAddr(ctx.channel());
        MqttProperties responseProperties = new MqttProperties();

    }


}
