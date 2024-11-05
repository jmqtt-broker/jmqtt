package org.jmqtt.starter.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jmqtt.broker.BrokerController;
import org.jmqtt.broker.acl.AuthValid;
import org.jmqtt.broker.processor.RequestProcessor;
import org.jmqtt.broker.processor.dispatcher.InnerMessageDispatcher;
import org.jmqtt.broker.remoting.netty.ChannelEventListener;
import org.jmqtt.broker.store.MessageStore;
import org.jmqtt.broker.store.SessionStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * @Description: java类作用描述
 * @Author: zhengtao
 * @CreateDate: 2024/11/4 16:23
 */
@Configuration
@Slf4j
public class JmqttStartup {

    @Autowired
    private ApplicationContext ctx;

    @PostConstruct
    public void start() {
        BrokerController ctrl = ctx.getBean(BrokerController.class);
        Optional.of(ctx.getBean(SessionStore.class)).ifPresent(ctrl::setSessionStore);
        Optional.of(ctx.getBean(MessageStore.class)).ifPresent(ctrl::setMessageStore);
        Optional.of(ctx.getBean(InnerMessageDispatcher.class)).ifPresent(ctrl::setInnerMessageDispatcher);

        Optional.of(ctx.getBean(ChannelEventListener.class)).ifPresent(ctrl::setChannelEventListener);
        Optional.of(ctx.getBeansOfType(RequestProcessor.class)).ifPresent(ctrlMap ->
            ctrlMap.values().forEach(ctrl::addRequestProcessor));
        ctrl.start();
    }

}
