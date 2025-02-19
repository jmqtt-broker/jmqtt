package org.jmqtt.keeper.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.pubsub.Topic;
import akka.cluster.ClusterEvent;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.common.akka.AkkaClusterEventListener;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.akka.KeeperSubscriber;
import org.jmqtt.keeper.config.KeeperAkkaConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeeperSystem {

    private final KeeperAkkaConfig keeperConfig;

    private ActorRef<Topic.Command<Event>> keeperChannel;

    private ActorRef<Event> keeper;

    private ActorRef<ClusterEvent.ClusterDomainEvent> clusterListener;

    private ActorSystem<Void> system;

    @PostConstruct
    public void start() {
        log.info("init akka");
        Config baseConfig = ConfigFactory.load();
        Config config;
        String systemName = AkkaConst.SYSTEM_NAME;
        config = ConfigFactory.parseString(keeperConfig.configStr()).withFallback(baseConfig);
        // Create an Akka system
        Behavior<Void> initBehavior = Behaviors.setup(
                context -> {
                    keeperChannel = context.spawn(Topic.create(Event.class, "jmqtt-keeper"), "JMqttKeeper");
                    keeper = context.spawn(Behaviors.setup(KeeperSubscriber::new), "KeeperSink");
                    keeperChannel.tell(Topic.subscribe(keeper));
                    clusterListener = context.spawn(Behaviors.setup(AkkaClusterEventListener::new), "ClusterListener");
                    return Behaviors.empty();
                });
        system = ActorSystem.create(initBehavior, systemName, config);
        log.info("akka system created.");
    }

}
