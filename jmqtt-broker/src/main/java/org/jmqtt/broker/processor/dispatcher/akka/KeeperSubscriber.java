package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;

@Slf4j
public class KeeperSubscriber extends AbstractBehavior<Event> {

    public KeeperSubscriber(ActorContext<Event> context) {
        super(context);
    }

    @Override
    public Receive<Event> createReceive() {
        return newReceiveBuilder().onMessage(Event.class, event -> {
            Object body = event.getBody();
            log.info("keeper receive message, {}", body);
            if (body instanceof BrokerInfo) {
                BrokerInfo brokerInfo = (BrokerInfo) body;
                BrokerDO broker = new BrokerDO(brokerInfo);
                BrokerContext.getLocalStore().storeBroker(broker);
            }
            return this;
        }).build();
    }
}
