package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.ActorSelection;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.common.config.NettyConfig;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.broker.store.highperformance.OutflowMessageHandler;
import org.jmqtt.broker.store.local.LocalStore;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class Receiver extends AbstractBehavior<Letter> {

    private Map<Integer, Consumer<Letter>> responseHandlerMap = new ConcurrentHashMap<>();

    public Receiver(ActorContext<Letter> context) {
        super(context);
        // responseHandlerMap.put(EventCode.BROKER_STATE.getCode(), this::brokerStatus);
        // responseHandlerMap.put(EventCode.BROKER_STATE_REQUEST.getCode(), this::reportBrokerInfo);
        // responseHandlerMap.put(EventCode.SESSION_STATE_RESPONSE.getCode(), this::onlineResponse);
        // responseHandlerMap.put(EventCode.SUBSCRIPTION_RESPONSE.getCode(), this::subscriptionResponse);
        // responseHandlerMap.put(EventCode.DISPATCHER_SHARE_SUBSCRIPTION_MSG.getCode(), this::dispatcherShareSubscriptionMsg);
        // responseHandlerMap.put(EventCode.KICK_CONNECTION.getCode(), this::kickConnection);
    }

    public static Behavior<Letter> create() {
        return Behaviors.setup(Receiver::new);
    }

    @Override
    public Receive<Letter> createReceive() {
        return newReceiveBuilder()
                .onMessage(Letter.class, letter -> {
                    Event event = letter.getMessage();
                    Consumer<Letter> eventHandler = responseHandlerMap.get(event.getEventCode());
                    if (eventHandler != null) {
                        eventHandler.accept(letter);
                    } else {
                        log.warn("[responseHandler] consume event is not supported,event:{}", event);
                    }
                    return this;
                }).build();
    }

    private void onlineResponse(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof OfflineMessageResponse) {
            OfflineMessageResponse res = (OfflineMessageResponse) body;
            String clientId = res.getClientId();
            Collection<Message> offlineList = res.getOfflineList();
            ClientSession clientSession = ConnectManager.getInstance().getClient(clientId);
            if (clientSession != null) {
                offlineList.forEach(msg -> {
                    MqttMessage mqttMessage = MessageUtil.getPubMessage(msg, false);
                    clientSession.getCtx().writeAndFlush(mqttMessage);
                });
            } else {
                log.warn("The client offline again, put the message to the offline queue,clientId:{}", clientId);
            }

        }
    }

}
