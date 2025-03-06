package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.ActorSelection;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.common.akka.Letter;

@Slf4j
public class TestKeeperSubscriber extends AbstractBehavior<Letter> {


    public TestKeeperSubscriber(ActorContext<Letter> context) {
        super(context);
    }

    public static Behavior<Letter> create() {
        return Behaviors.setup(TestKeeperSubscriber::new);
    }

    @Override
    public Receive<Letter> createReceive() {
        return newReceiveBuilder().onMessage(Letter.class, letter -> {
            log.info("[TestKeeperSubscriber]:{}", letter);
            ActorSelection selection = Adapter.toClassic(getContext().getSystem())
                    .actorSelection(letter.getResponsePath());
            Letter res = new Letter();
            res.setResponsePath("hello world");
            selection.tell(res, Adapter.toClassic(getContext().getSelf()));
            return this;
        }).build();
    }

}
