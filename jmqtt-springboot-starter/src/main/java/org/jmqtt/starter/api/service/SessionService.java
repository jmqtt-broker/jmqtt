package org.jmqtt.starter.api.service;

import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.processor.dispatcher.akka.ClusterHelper;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.event.EventCode;
import org.jmqtt.starter.api.entity.PageVo;

import java.util.Optional;

public interface SessionService {

    SessionDO selectByClientId(String clientId);

    PageVo<SessionDO> page(int page, int pageSize, SessionDO sessionDO);

    default void kickConnection(String clientId) {
        SessionDO session = selectByClientId(clientId);
        if (BrokerContext.getBrokerId().equals(session.getBrokerId())) {
            Optional.ofNullable(ConnectManager.getInstance().getClient(clientId)).ifPresent(s -> {
                Mqtt5Utils.sendDisconnectAndClose(s, (byte) 0x8B);
            });
        } else if (ClusterHelper.lightning()) {
            Letter letter = new Letter(ClusterHelper.getEvent(EventCode.KICK_CONNECTION, clientId));
            ClusterHelper.sendByPath(letter, session.getBrokerId());
        } else {
            ClusterHelper.sendToKeeper(ClusterHelper.getEvent(EventCode.KICK_CONNECTION, clientId));
        }
    }

}
