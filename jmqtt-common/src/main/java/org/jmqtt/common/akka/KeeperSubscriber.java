package org.jmqtt.common.akka;

import akka.actor.ActorPath;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.ClusterCommand;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.common.event.Event;

@Slf4j
public class KeeperSubscriber extends AbstractBehavior<Event> {

    public KeeperSubscriber(ActorContext<Event> context) {
        super(context);
    }

    @Override
    public Receive<Event> createReceive() {
        return newReceiveBuilder().onMessage(Event.class, event -> {
            log.info("keeper receive message, {}", event.getBody());
            Cluster cluster = Cluster.get(getContext().getSystem());
            ActorRef<ClusterCommand> manager = cluster.manager();
            ActorPath path = manager.path();
            Member member = cluster.selfMember();
            return this;
        }).build();
    }
}
