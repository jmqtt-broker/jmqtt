package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.store.local.LocalStore;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.event.EventCode;

public class AkkaClusterEventListener extends AbstractBehavior<ClusterEvent.ClusterDomainEvent> {

    public AkkaClusterEventListener(ActorContext<ClusterEvent.ClusterDomainEvent> context) {
        super(context);
        // 订阅集群事件
        Cluster.get(context.getSystem()).subscriptions()
                .tell(Subscribe.create(context.getSelf(), ClusterEvent.ClusterDomainEvent.class));
    }

    public static Behavior<ClusterEvent.ClusterDomainEvent> create() {
        return Behaviors.setup(AkkaClusterEventListener::new);
    }

    @Override
    public Receive<ClusterEvent.ClusterDomainEvent> createReceive() {
        return newReceiveBuilder()
                .onMessage(ClusterEvent.MemberJoined.class, this::onMemberJoined)
                .onMessage(ClusterEvent.MemberRemoved.class, this::onMemberRemoved)
                .onMessage(ClusterEvent.MemberDowned.class, this::onMemberDowned)
                .onMessage(ClusterEvent.UnreachableMember.class, this::onUnreachableMember)
                .build();
    }

    private Behavior<ClusterEvent.ClusterDomainEvent> onMemberJoined(ClusterEvent.MemberJoined event) {
        getContext().getLog().info("Member joined: {}", event.member());
        if (Cluster.get(getContext().getSystem()).selfMember().hasRole(AkkaConst.KEEPER)) {
            Address address = event.member().address();
            AkkaClusterEventHandler handler = (AkkaClusterEventHandler) BrokerContext.getBrokerController().getClusterEventHandler();
            ActorSelection selection = Adapter.toClassic(getContext().getSystem())
                    .actorSelection(handler.getReceiver().path().toStringWithAddress(address));
            Letter letter = new Letter(ClusterHelper.getEvent(EventCode.BROKER_STATE_REQUEST, true));
            letter.setResponsePath(handler.getSelfPathWithAddress());
            selection.tell(letter, Adapter.toClassic(getContext().getSelf()));
        }
        return this;
    }

    private Behavior<ClusterEvent.ClusterDomainEvent> onMemberRemoved(ClusterEvent.MemberRemoved event) {
        Member member = event.member();
        getContext().getLog().info("Member removed: {}", member);
        if (Cluster.get(getContext().getSystem()).selfMember().hasRole(AkkaConst.KEEPER)) {
            Address address = member.address();
            String brokerId = address.toString();
            LocalStore localStore = BrokerContext.getBrokerController().getLocalStore();
            BrokerDO brokerDO = localStore.getBroker(brokerId);
            if (brokerDO != null) {
                brokerDO.setStatus(false);
                brokerDO.setOfflineAt(System.currentTimeMillis());
                localStore.storeBroker(brokerDO);
            }
        }
        return this;
    }

    private Behavior<ClusterEvent.ClusterDomainEvent> onMemberDowned(ClusterEvent.MemberDowned event) {
        getContext().getLog().info("Member downed: {}", event.member());
        return this;
    }

    private Behavior<ClusterEvent.ClusterDomainEvent> onUnreachableMember(ClusterEvent.UnreachableMember event) {
        getContext().getLog().info("Member unreachable: {}", event.member());
        return this;
    }

}
