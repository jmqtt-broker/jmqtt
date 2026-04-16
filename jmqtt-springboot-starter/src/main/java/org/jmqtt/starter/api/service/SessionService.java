package org.jmqtt.starter.api.service;

import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.processor.dispatcher.akka.ClusterHelper;
import org.jmqtt.broker.processor.protocol.mqtt5.Mqtt5Utils;
import org.jmqtt.broker.remoting.session.ConnectManager;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.broker.store.rdb.daoobject.SubscriptionDO;
import org.jmqtt.common.event.EventCode;
import org.jmqtt.starter.api.entity.PageVo;

import java.util.List;
import java.util.Optional;

public interface SessionService {

    SessionDO selectByClientId(String clientId);

    PageVo<SessionDO> page(int page, int pageSize, SessionDO sessionDO);

    default void kickConnection(String clientId) {
        SessionDO session = selectByClientId(clientId);
        String brokerId = session.getBrokerId();
        if (BrokerContext.getBrokerId().equals(brokerId)) {
            Optional.ofNullable(ConnectManager.getInstance().getClient(clientId)).ifPresent(s -> {
                Mqtt5Utils.sendDisconnectAndClose(s, (byte) 0x8B, "Server terminated connection.");
            });
        } else if (ClusterHelper.lightning()) {
            BrokerDO broker = BrokerContext.getLocalStore().getBroker(brokerId);
            if (Optional.ofNullable(broker).map(BrokerDO::getStatus).orElse(false)) {
                ClusterHelper.sendEvent(ClusterHelper.getEvent(EventCode.KICK_CONNECTION, clientId), brokerId);
            }
        } else {
            ClusterHelper.sendToCluster(ClusterHelper.getEvent(EventCode.KICK_CONNECTION, clientId));
        }
    }

    List<SubscriptionDO> getSubscriptions(String clientId);

    Message getWillMessage(String clientId);

}
