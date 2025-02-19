package org.jmqtt.broker.processor.dispatcher.akka;

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
import org.jmqtt.broker.common.config.AkkaConfig;
import org.jmqtt.broker.common.config.BrokerConfig;
import org.jmqtt.broker.common.log.JmqttLogger;
import org.jmqtt.broker.processor.dispatcher.ClusterEventHandler;
import org.jmqtt.broker.processor.dispatcher.EventConsumeHandler;
import org.jmqtt.common.akka.AkkaClusterEventListener;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.KeeperSubscriber;
import org.jmqtt.common.event.Event;
import org.slf4j.Logger;

import java.util.List;

public class AkkaClusterEventHandler implements ClusterEventHandler {

    private static final Logger log = JmqttLogger.eventLog;
    private ActorRef<Command<Event>> topic;
    private ActorRef<Event> subscriber;
    private ActorSystem<Void> system;
    private EventConsumeHandler eventConsumeHandler;

    private ActorRef<Topic.Command<Event>> keeperTopic;
    private ActorRef<Event> keeperSubscriber;
    private ActorRef<ClusterEvent.ClusterDomainEvent> clusterListener;

    public AkkaClusterEventHandler() {

    }

    @Override
    public void start(BrokerConfig brokerConfig) {
        log.info("init akka");
        Config baseConfig = ConfigFactory.load();
        AkkaConfig akkaConfig = brokerConfig.getAkka();
        Config config;
        String systemName = AkkaConst.SYSTEM_NAME;
        if (akkaConfig != null) {
            config = ConfigFactory.parseString(akkaConfig.configStr()).withFallback(baseConfig);
            systemName = akkaConfig.getSystemName();
        } else {
            config = baseConfig;
        }
        Behavior<Void> initBehavior = Behaviors.setup(
                context -> {
                    // 广播给所有节点的topic
                    topic = context.spawn(Topic.create(Event.class, "jmqtt-event"), "ClusterEvent");
                    subscriber = context.spawn(Subscriber.create(this.eventConsumeHandler), "ClusterEventSubscriber");
                    topic.tell(Topic.subscribe(subscriber));
                    // 广播给拥有keeper角色的节点
                    keeperTopic = context.spawn(Topic.create(Event.class, "jmqtt-keeper"), "KeeperTopic");
                    if (Cluster.get(context.getSystem()).selfMember().hasRole(AkkaConst.KEEPER)) {
                        keeperSubscriber = context.spawn(Behaviors.setup(KeeperSubscriber::new), "KeeperSubscriber");
                        keeperTopic.tell(Topic.subscribe(keeperSubscriber));
                    }
                    clusterListener = context.spawn(Behaviors.setup(AkkaClusterEventListener::new), "ClusterListener");
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
        keeperTopic.tell(Topic.publish(event));
    }
}
