package com.jmqtt.core.mqtt.control.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;

public interface ConnectHandler {

    ConnectHandler versionVerify(int mqttVersion);

    ConnectHandler clientIdVerify(String clientId);

    ConnectHandler blackListVerify(String remoteAddr, String clientId);

    ConnectHandler authentication(String clientId, String userName, byte[] password, String defaultUser, String defaultPwd, boolean anonymousEnable);

    ConnectHandler keepAliveVerify(String clientId, ChannelHandlerContext ctx, int heatbeatSec);

    void verifySuccess(ChannelHandlerContext ctx, String clientId, boolean cleanStart, String remoteAddress, MqttConnectMessage connectMessage);

}
