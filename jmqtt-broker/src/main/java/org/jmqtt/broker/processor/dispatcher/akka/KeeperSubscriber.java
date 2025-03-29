package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.ActorSelection;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.model.*;
import org.jmqtt.broker.store.SessionState;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.broker.store.rdb.daoobject.SessionDO;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 用于非keeper节点向keeper节点单向通知
 */
@Slf4j
public class KeeperSubscriber extends AbstractBehavior<Letter> {

    private Map<Integer, Consumer<Letter>> eventHandlerMap = new ConcurrentHashMap<>();

    public KeeperSubscriber(ActorContext<Letter> context) {
        super(context);
        eventHandlerMap.put(EventCode.BROKER_STATE.getCode(), this::brokerOnline);
        eventHandlerMap.put(EventCode.SESSION_STATE.getCode(), this::sessionState);
        eventHandlerMap.put(EventCode.STORE_RETAIN_MSG.getCode(), this::storeRetain);
        eventHandlerMap.put(EventCode.CLEAR_RETAIN_MSG.getCode(), this::clearRetain);
        eventHandlerMap.put(EventCode.SUBSCRIPTION.getCode(), this::subscription);
        eventHandlerMap.put(EventCode.UNSUBSCRIPTION.getCode(), this::unSubscription);
        eventHandlerMap.put(EventCode.DISPATCHER_FOR_SHARE_SUBSCRIPTION.getCode(), this::dispatcherMsgForShareSubscription);
    }

    public static Behavior<Letter> create() {
        return Behaviors.setup(KeeperSubscriber::new);
    }

    @Override
    public Receive<Letter> createReceive() {
        return newReceiveBuilder().onMessage(Letter.class, letter -> {
            Event event = letter.getMessage();
            Consumer<Letter> eventHandler = eventHandlerMap.get(event.getEventCode());
            if (eventHandler != null) {
                eventHandler.accept(letter);
            } else {
                log.warn("[EventConsumeHandler] consume event is not supported,event:{}", event);
            }
            return this;
        }).build();
    }

    private void brokerOnline(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        log.info("keeper receive broker status, {}", body);
        if (body instanceof BrokerInfo) {
            BrokerInfo brokerInfo = (BrokerInfo) body;
            BrokerDO broker = new BrokerDO(brokerInfo);
            BrokerContext.getLocalStore().storeBroker(broker);
        }
    }

    private void sessionState(Letter letter) {
        Event event = letter.getMessage();
        if (letter.isSync() && BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            return;
        }
        Object body = event.getBody();
        log.info("keeper receive session status, {}", body);
        if (body instanceof SessionState) {
            SessionState sessionState = (SessionState) body;
            if (!BrokerContext.getBrokerId().equals(event.getFromBroker())) {
                BrokerContext.getSessionStore().storeSession(sessionState.getClientId(), sessionState);
            }
            String responsePath = letter.getResponsePath();
            if (StringUtils.isBlank(responsePath)) {
                if (sessionState.getState() == SessionState.StateEnum.ONLINE) {
                    // 如果是客户端上线，需要查看客户端是否存在离线消息，存在则返回
                    String clientId = sessionState.getClientId();
                    Collection<Message> offlineList = BrokerContext.getSessionStore().getAllOfflineMsg(clientId);
                    if (!offlineList.isEmpty()) {
                        BrokerContext.getSessionStore().clearOfflineMsg(clientId);
                        Letter res = new Letter(ClusterHelper.getEvent(EventCode.SESSION_STATE_RESPONSE,
                                new OfflineMessageResponse(clientId, offlineList)));
                        response(res, responsePath);
                    }
                }
            }
            if (!letter.isSync()) {
                // 同步到其他keeper节点
                ClusterHelper.syncToKeeper(letter);
            }
        }
    }

    private void storeRetain(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof Message) {
            Message message = (Message) body;
            String topic = (String) message.getHeader(MessageHeader.TOPIC);
            BrokerContext.getMessageStore().storeRetainMessage(topic, message);
        }
    }

    private void clearRetain(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof Message) {
            Message message = (Message) body;
            String topic = (String) message.getHeader(MessageHeader.TOPIC);
            BrokerContext.getMessageStore().clearRetainMessage(topic);
        }
    }

    private void subscription(Letter letter) {
        Event event = letter.getMessage();
        if (letter.isSync() && BrokerContext.getBrokerId().equals(event.getFromBroker())) {
            return;
        }
        Subscription subscription = (Subscription) event.getBody();
        boolean subRes = BrokerContext.getSubscriptionMatcher().subscribe(subscription);
        BrokerContext.getSessionStore().storeSubscription(subscription.getClientId(), subscription);
        String responsePath = letter.getResponsePath();
        if (StringUtils.isNotBlank(responsePath)) {
            // 如果存在保留消息，返回保留消息给订阅节点，由订阅节点转发给客户端
            Collection<Message> retainMsgList = BrokerContext.getMessageStore().getRetainMsg(subscription.getTopic());
            if (!retainMsgList.isEmpty()) {
                Letter res = new Letter(ClusterHelper.getEvent(EventCode.SUBSCRIPTION_RESPONSE,
                        new SubscriptionRetainMessage(retainMsgList, subscription, subRes)));
                response(res, responsePath);
            }
            if (!letter.isSync()) {
                ClusterHelper.syncToKeeper(letter);
            }
        }
    }

    private void unSubscription(Letter letter) {
        Object body = letter.getMessage().getBody();
        if (body instanceof Subscription) {
            Subscription subscription = (Subscription) body;
            String clientId = subscription.getClientId();
            String topic = subscription.getTopic();
            BrokerContext.getSubscriptionMatcher().unSubscribe(topic, clientId);
            BrokerContext.getSessionStore().delSubscription(clientId, topic);
        }
    }

    private void dispatcherMsgForShareSubscription(Letter letter) {
        Event event = letter.getMessage();
        Object body = event.getBody();
        if (body instanceof Message) {
            Message message = (Message) body;
            String topic = (String) message.getHeader(MessageHeader.TOPIC);
            String clientId = message.getClientId();
            Set<Subscription> shareSubscriptions = BrokerContext.getSubscriptionMatcher().shareSubscription(topic, clientId);
            if (!shareSubscriptions.isEmpty()) {
                Set<String> clientIds = shareSubscriptions.stream().map(Subscription::getClientId).collect(Collectors.toSet());
                List<SessionDO> sessionList = BrokerContext.getSessionStore().getSessionList(clientIds);
                Map<String, List<String>> clientMap = sessionList.stream().filter(s -> SessionState.StateEnum.ONLINE.getCode().equals(s.getState())).collect(
                        Collectors.groupingBy(SessionDO::getBrokerId, Collectors.mapping(SessionDO::getClientId, Collectors.toList())));
                clientMap.forEach((brokerId, clientIdList) -> {
                    String responsePath = brokerId + "/user/" + AkkaConst.AKKA_RECEIVER;
                    Letter res = new Letter(ClusterHelper.getEvent(EventCode.DISPATCHER_SHARE_SUBSCRIPTION_MSG,
                            new ShareSubscriptionMsg(shareSubscriptions.stream().filter(s -> clientIdList.contains(s.getClientId())).collect(Collectors.toList()), message)));
                    response(res, responsePath);
                });
            }
        }
    }

    private void response(Letter res, String responsePath) {
        log.info("responsePath: {}", responsePath);
        ActorSelection selection = Adapter.toClassic(getContext().getSystem())
                .actorSelection(responsePath);
        selection.tell(res, Adapter.toClassic(getContext().getSelf()));
    }
}
