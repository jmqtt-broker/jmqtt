package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.log.LogUtil;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.OfflineMessageResponse;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.common.model.SubscriptionRetainMessage;
import org.jmqtt.broker.remoting.session.ClientSession;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.remoting.util.MessageUtil;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class Receiver extends AbstractBehavior<Letter> {

    private Map<Integer, Consumer<Letter>> responseHandlerMap = new ConcurrentHashMap<>();

    public Receiver(ActorContext<Letter> context) {
        super(context);
        responseHandlerMap.put(EventCode.SESSION_STATE_RESPONSE.getCode(), this::onlineResponse);
        responseHandlerMap.put(EventCode.SUBSCRIPTION_RESPONSE.getCode(), this::subscriptionResponse);
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

    private void subscriptionResponse(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof SubscriptionRetainMessage) {
            SubscriptionRetainMessage sm = (SubscriptionRetainMessage) body;
            Subscription subscription = sm.getSubscription();
            String clientId = subscription.getClientId();
            if (!ClusterHelper.isKeeper()) {
                BrokerContext.getSessionStore().storeSubscription(clientId, subscription);
                BrokerContext.getSubscriptionMatcher().subscribe(subscription);
            }
            BrokerContext.getRetainMessageDispatcher().appendMessage(sm);
        }
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
