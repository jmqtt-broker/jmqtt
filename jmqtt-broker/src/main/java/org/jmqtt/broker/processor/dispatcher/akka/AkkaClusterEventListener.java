package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ClusterEvent;

public class AkkaClusterEventListener extends AbstractBehavior<ClusterEvent.ClusterDomainEvent> {

    public AkkaClusterEventListener(ActorContext<ClusterEvent.ClusterDomainEvent> context) {
        super(context);
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
        return this;
    }

    private Behavior<ClusterEvent.ClusterDomainEvent> onMemberRemoved(ClusterEvent.MemberRemoved event) {
        getContext().getLog().info("Member removed: {}", event.member());
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
