package org.jmqtt.common.akka;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;

public class AkkaClusterEventListener extends AbstractBehavior<ClusterEvent.ClusterDomainEvent> {

    public AkkaClusterEventListener(ActorContext<ClusterEvent.ClusterDomainEvent> context) {
        super(context);
        // 订阅集群事件
        // Cluster cluster = Cluster.get(context.getSystem());
        // cluster.subscriptions().tell(Subscribe.create(context.getSelf(), ClusterEvent.ClusterDomainEvent.class));
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
