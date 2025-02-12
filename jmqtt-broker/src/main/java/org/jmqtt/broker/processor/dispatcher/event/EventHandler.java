package org.jmqtt.broker.processor.dispatcher.event;

@FunctionalInterface
public interface EventHandler {

    void handle(Event event);

}
