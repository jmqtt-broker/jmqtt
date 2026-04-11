package org.jmqtt.broker.common.future;

public interface Listener<T> {

    void handle(T res);

}
