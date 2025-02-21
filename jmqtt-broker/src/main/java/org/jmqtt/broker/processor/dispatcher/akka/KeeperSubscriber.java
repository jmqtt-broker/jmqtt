package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.processor.dispatcher.event.EventHandler;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KeeperSubscriber extends AbstractBehavior<Event> {

    private Map<Integer, EventHandler> eventHandlerMap = new ConcurrentHashMap<>();

    public KeeperSubscriber(ActorContext<Event> context) {
        super(context);
        eventHandlerMap.put(EventCode.BROKER_STATE.getCode(), this::brokerOnline);
        eventHandlerMap.put(EventCode.SESSION_STATE.getCode(), this::sessionState);
    }

    @Override
    public Receive<Event> createReceive() {
        return newReceiveBuilder().onMessage(Event.class, event -> {
            EventHandler eventHandler = eventHandlerMap.get(event.getEventCode());
            if (eventHandler != null) {
                eventHandler.handle(event);
            } else {
                log.warn("[EventConsumeHandler] consume event is not supported,event:{}", event);
            }
            return this;
        }).build();
    }

    private void brokerOnline(Event event) {
        Object body = event.getBody();
        log.info("keeper receive message, {}", body);
        if (body instanceof BrokerInfo) {
            BrokerInfo brokerInfo = (BrokerInfo) body;
            BrokerDO broker = new BrokerDO(brokerInfo);
            BrokerContext.getLocalStore().storeBroker(broker);
        }
    }

    private void sessionState(Event event) {
        Object body = event.getBody();
        log.info("keeper receive message, {}", body);
        if (BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            log.debug("Event from current node,ignore the event,fromBroker:{}", event.getFromBroker());
            return;
        }
        if (body instanceof SessionState) {
            SessionState sessionState = (SessionState) body;
            BrokerContext.getLocalStore().storeSession(sessionState);
        }
    }
}
