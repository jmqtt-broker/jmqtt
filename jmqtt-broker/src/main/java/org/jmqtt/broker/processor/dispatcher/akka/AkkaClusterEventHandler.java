package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.Address;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.pubsub.Topic;
import akka.actor.typed.pubsub.Topic.Command;
import akka.cluster.ClusterEvent;
import akka.cluster.typed.Cluster;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.NoArgsConstructor;
import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.EventConsumeHandler;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.event.Event;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

@NoArgsConstructor
public class AkkaClusterEventHandler implements ClusterEventHandler {

    private static final Logger log = JmqttLogger.eventLog;
    private ActorRef<Command<Event>> topic;
    private ActorRef<Event> subscriber;
    private ActorSystem<Void> system;
    private EventConsumeHandler eventConsumeHandler;

    private ActorRef<Topic.Command<Letter>> keeperTopic;
    private ActorRef<Letter> keeperSubscriber;
    private ActorRef<ClusterEvent.ClusterDomainEvent> clusterListener;

    private ActorRef<Letter> receiver;

    private String selfPathWithAddress;

    private String selfPath;

    @Override
    public void start(BrokerConfig brokerConfig) {
        log.info("init akka");
        Config baseConfig = ConfigFactory.load();
        AkkaConfig akkaConfig = Optional.ofNullable(brokerConfig.getAkka()).orElse(new AkkaConfig());
        Config config = ConfigFactory.parseString(akkaConfig.configStr()).withFallback(baseConfig);
        String systemName = akkaConfig.getSystemName();
        Behavior<Void> initBehavior = Behaviors.setup(
                context -> {
                    // 广播给所有节点的topic
                    topic = context.spawn(Topic.create(Event.class, "jmqtt-event"), "ClusterEvent");
                    subscriber = context.spawn(Subscriber.create(this.eventConsumeHandler), "ClusterEventSubscriber");
                    topic.tell(Topic.subscribe(subscriber));
                    // 广播给拥有keeper角色的节点
                    keeperTopic = context.spawn(Topic.create(Letter.class, "jmqtt-keeper"), "KeeperTopic");
                    if (Cluster.get(context.getSystem()).selfMember().hasRole(AkkaConst.KEEPER)) {
                        keeperSubscriber = context.spawn(KeeperSubscriber.create(), "KeeperSubscriber");
                        keeperTopic.tell(Topic.subscribe(keeperSubscriber));
                        // clusterListener = context.spawn(Behaviors.setup(AkkaClusterEventListener::new), "ClusterListener");
                    }
                    receiver = context.spawn(Receiver.create(), "AkkaReceiver");
                    Address address = new Address(AkkaConst.SYSTEM_PROTOCOL, akkaConfig.getSystemName(), akkaConfig.getHost(), Integer.parseInt(akkaConfig.getPort()));
                    selfPathWithAddress = receiver.path().toStringWithAddress(address);
                    selfPath = receiver.path().toString();
                    return Behaviors.empty();
                });
        system = ActorSystem.create(initBehavior, systemName, config);
        log.info("akka system created.");
    }

    @Override
    public void shutdown() {
        topic.tell(Topic.unsubscribe(subscriber));
    }

    @Override
    public boolean sendEvent(Event event) {
        topic.tell(Topic.publish(event));
        return true;
    }

    @Override
    public void setEventConsumeHandler(EventConsumeHandler eventConsumeHandler) {
        this.eventConsumeHandler = eventConsumeHandler;
    }

    @Override
    public List<Event> pollEvent(int maxPollNum) {
        return null;
    }

    @Override
    public void sendTokeeper(Event event) {
        String path = Cluster.get(system).selfMember().hasRole(AkkaConst.KEEPER) ? selfPath : selfPathWithAddress;
        Letter letter = new Letter(event, path);
        keeperTopic.tell(Topic.publish(letter));
    }

}
