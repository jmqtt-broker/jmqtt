package org.jmqtt.broker.processor.dispatcher.event;

import org.jmqtt.common.event.Event;

@FunctionalInterface
public interface EventHandler {

    void handle(Event event);

}
