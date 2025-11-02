package org.jmqtt.broker.common.future;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2025/10/14 11:45
 */
public interface Listener<T> {

    void handle(T res);

}
