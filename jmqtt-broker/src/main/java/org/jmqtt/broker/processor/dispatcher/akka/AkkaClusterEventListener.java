package org.jmqtt.broker.processor.dispatcher.akka;

import akka.actor.Address;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import org.jmqtt.broker.common.helper.BrokerContext;
import org.jmqtt.broker.common.helper.MixAll;
import org.jmqtt.broker.common.model.Message;
import org.jmqtt.broker.common.model.Subscription;
import org.jmqtt.broker.common.model.SubscriptionSyncDTO;
import org.jmqtt.broker.store.local.LocalStore;
import org.jmqtt.broker.store.rdb.daoobject.BrokerDO;
import org.jmqtt.common.akka.AkkaConst;
import org.jmqtt.common.akka.Letter;
import org.jmqtt.common.entity.BrokerInfo;
import org.jmqtt.common.event.Event;
import org.jmqtt.common.event.EventCode;
import scala.collection.Iterable;
import scala.math.Ordering;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

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
        Member joinMember = event.member();
        getContext().getLog().info("Member joined: {}", joinMember);
        Cluster cluster = Cluster.get(getContext().getSystem());
        Member self = cluster.selfMember();
        if (!self.equals(joinMember)) {
            String addr = joinMember.address().toString();
            // 当前broker信息需要同步给刚加入集群的节点
            syncBrokerInfo(addr);
            if (ClusterHelper.isEarliestKeeper()) {
                // 同步有效会话的Client、所有订阅关系、所有retain消息、离线消息
                if (joinMember.hasRole(AkkaConst.KEEPER)) {
                    syncOnlineClient(addr);
                    syncRetainMessage(addr);
                    syncOfflineMessage(addr);
                }
                // 同步订阅关系
                syncSubscription(addr);
            }
        }
        return this;
    }

    private Behavior<ClusterEvent.ClusterDomainEvent> onMemberRemoved(ClusterEvent.MemberRemoved event) {
        Member member = event.member();
        getContext().getLog().info("Member removed: {}", member);
        if (!Cluster.get(getContext().getSystem()).selfMember().equals(member)) {
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

    private void syncBrokerInfo(String address) {
        BrokerDO broker = BrokerContext.getLocalStore().getBroker(BrokerContext.getBrokerId());
        BrokerInfo info = new BrokerInfo();
        MixAll.copyProperties(broker, info);
        Event e = ClusterHelper.getEvent(EventCode.BROKER_ONLINE, info);
        ClusterHelper.sendToCluster(e, address);

    }

    private void syncRetainMessage(String address) {
        Collection<Message> retains = BrokerContext.getMessageStore().getAllRetainMsg();
        if (!retains.isEmpty()) {
            retains.forEach(m -> {
                ClusterHelper.sendLetter(Letter.of(ClusterHelper.getEvent(EventCode.STORE_RETAIN_MSG, m)), address);
            });
        }
    }

    public void syncOnlineClient(String address) {
        // BrokerContext.getSessionStore().getSession()
    }

    public void syncOfflineMessage(String address) {
        // BrokerContext.getMessageStore()
    }

    public void syncSubscription(String address) {
        Set<Subscription> subscriptionSet = BrokerContext.getSessionStore().getAllSubscription();
        if (!subscriptionSet.isEmpty()) {
            ClusterHelper.sendEvent(ClusterHelper.getEvent(EventCode.SUBSCRIPTION_SYNC, new SubscriptionSyncDTO(subscriptionSet)), address);
        }
    }

}
