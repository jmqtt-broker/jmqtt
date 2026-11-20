# JMQTT 架构设计文档

> 版本：v1.0 | 最后更新：2026-11-19

## 1. 系统定位

**JMQTT** 是一个去中心化分布式 MQTT Broker，采用 Java 21 + Netty + Hazelcast 构建。

- **协议支持**：MQTT 3.1、MQTT 3.1.1、MQTT 5.0 三版本统一接入，按协议等级分流处理
- **传输层**：TCP / WebSocket / SSL-TCP / SSL-WebSocket，四端口独立监听
- **集群模式**：基于 Hazelcast，支持多节点消息转发、会话同步
- **持久化**：内置 H2 文件数据库（MyBatis-Plus），会话/消息/订阅可持久化

---

## 2. 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端层                                │
│      MQTTX / mosquitto_pub / 自定义 MQTT 客户端                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │ TCP / WebSocket / SSL
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Netty 网络层                                │
│  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐          │
│  │ TCP:1883│  │ WS :1884│  │SSL-TCP   │  │SSL-WS    │          │
│  └────┬────┘  └────┬────┘  └─────┬────┘  └─────┬────┘          │
│       └────────────┴─────────────┴─────────────┘               │
│                              │                                  │
│                    NettyMqttHandler                              │
│                   (协议解码 → VirtualThread)                      │
│                              │                                  │
└──────────────────────────────┼──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      协议处理层                                  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    BaseCtrl 基类                          │    │
│  │    ctrlTable: Map<MqttMessageType, BaseCtrl>             │    │
│  └─────────────────────────┬───────────────────────────────┘    │
│                            │                                     │
│  ┌─────────────────────────┼───────────────────────────────┐    │
│  │                         │                               │    │
│  ▼                         ▼                               ▼    │
│ ConnectCtrl             PublishCtrl                    SubscribeCtrl  ...
│ (CONNECT)               (PUBLISH)                     (SUBSCRIBE)      │
│                            │                                     │
│              ┌─────────────┴──────────────┐                     │
│              │                             │                      │
│     MQTT 3.1/3.1.1                  MQTT 5.0                      │
│     Mqtt_3_4_xxx                   Mqtt_5_xxx                    │
│     (版本差异化处理)               (extends Mqtt_3_4_xxx)          │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      消息分发层                                  │
│                                                                  │
│                  MessageDispatcher                               │
│          LinkedBlockingQueue + VirtualThread                     │
│                                                                  │
│          ┌───── DispatcherTask (批量出队) ──────┐                │
│          │                                      │                │
│          ▼                                      ▼                │
│  ┌───────────────┐                  ┌───────────────┐           │
│  │ 订阅匹配       │                  │ ReceiveMaximum │           │
│  │ (主题树)       │                  │ (MQTT 5 配额)  │           │
│  └───────┬───────┘                  └───────┬───────┘           │
│          │                                  │                    │
│          └──────────────┬───────────────────┘                    │
│                         ▼                                        │
│               write() → Netty Channel                            │
└──────────────────────────────┬──────────────────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│   在线客户端      │ │    离线消息      │ │   集群转发        │
│  直接发送         │ │  H2 持久化       │ │  Hazelcast Queue │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

---

## 3. 模块职责

### 3.1 模块依赖关系

```
jmqtt-core (唯一子模块)
├── cluster/         ← Hazelcast 集群通信 (依赖 mqtt 包的 DTO)
├── common/          ← 通用常量、线程工厂、CacheHelper
├── config/          ← YAML 配置加载
├── exception/       ← 自定义异常
├── netty/           ← 网络层 (TCP/WS/SSL)
│   ├── session/     ← ClientSession, ClientManager
│   ├── coder/       ← WebSocket 编解码
│   ├── config/      ← NettyConfig, TLS
│   ├── event/       ← ChannelEvent 事件模型
│   └── helper/      ← ChannelUtil, ChannelHelper
└── mqtt/            ← ⭐ 核心协议层
    ├── auth/        ← 认证接口
    ├── config/      ← BrokerConfig
    ├── control/     ← 报文处理 Ctrl 组
    │   ├── handler/     ← 版本差异化 Handler
    │   ├── dispatcher/  ← MessageDispatcher
    │   └── resend/      ← 重连补发服务
    ├── helper/      ← Reason, MessageUtil, ReceiveMaximumManager
    ├── model/       ← Message, Subscription, SubscriptionOption
    ├── store/       ← 存储层
    │   └── persist/     ← MyBatis-Plus 持久化
    ├── subscribe/   ← 主题树匹配
    └── timer/       ← 定时调度（Netty 时间轮）
```

**依赖方向**：`netty → mqtt ← cluster`，三层通过 `JmqttContext` 静态上下文桥接。

### 3.2 核心类职责表

| 类 | 职责 | 生命周期 |
|----|------|----------|
| `JmqttBroker` | 总启动器，持有所有组件引用 | 单例（main 启动，shutdown 关闭） |
| `JmqttContext` | 静态上下文门面，持有 broker/config/store/dispatcher 等全局引用 | 单例（与 Broker 同生命周期） |
| `NettyServer` | 四端口 Netty 服务启动 | 单例 |
| `BaseCtrl` | MQTT 报文处理基类，按版本分发 handler | 10 个实例（每种报文类型一个） |
| `MessageDispatcher` | 异步批量消息分发器 | 单例 |
| `ReceiveMaximumManager` | MQTT 5 出站/入站配额管理 | 静态方法调用，内部按 clientId 维护 Quota |
| `DefaultSubscriptionTreeMatcher` | 主题树订阅匹配 | 单例 |
| `LocalSessionStore` | 会话/订阅/QoS 消息缓存（内存 + H2） | 单例 |
| `LocalMessageStore` | 遗嘱/保留消息（内存 + H2） | 单例 |
| `ScheduleTaskScheduler` | 定时任务调度器（HashedWheelTimer） | 单例 |
| `JmqttCluster` | Hazelcast 集群管理 | 静态工具类 |
| `ClusterMessageDispatcher` | 集群消息收发 | 单例 |

---

## 4. 启动流程

```
main()
  │
  ▼
JmqttBroker.start()
  │
  ├── 1. 加载 jmqtt.yml → JmqttConfig (broker + netty + cluster)
  │
  ├── 2. 初始化 Store
  │      ├── LocalDB.startDB()  ← H2 数据库启动，加载 sql/jmqtt_local.sql
  │      ├── LocalSessionStore  ← 内存缓存初始化
  │      └── LocalMessageStore  ← 遗嘱/保留消息缓存
  │
  ├── 3. 初始化主题匹配器
  │      └── DefaultSubscriptionTreeMatcher ← 空树
  │
  ├── 4. 初始化认证
  │      └── DefaultJmqttAuth ← 空实现（走 broker 配置的 user/password）
  │
  ├── 5. 初始化消息分发器
  │      └── MessageDispatcher ← LinkedBlockingQueue + VirtualThread Executor
  │
  ├── 6. 初始化定时任务
  │      └── TimerUtil.start() → ScheduleTaskScheduler.start() → reload()
  │
  ├── 7. 初始化集群（可选）
  │      └── JmqttCluster.start() → Hazelcast.newHazelcastInstance()
  │
  ├── 8. 注册 Ctrl
  │      └── 10 个 BaseCtrl 子类 → JmqttContext.registerCtrl(MqttMessageType, ctrl)
  │
  └── 9. 启动 Netty 服务
         └── NettyServer.start() → TCP(1883) / WS(1884) / SSL(8883) / SSL-WS(8884)
```

---

## 5. 消息处理链路

### 5.1 入站（客户端 → Broker）

```
Netty TCP/WS Pipeline
  │
  ▼
MqttDecoder → MqttMessage
  │
  ▼
NettyMqttHandler.channelRead(ctx, msg)
  │
  └── Thread.startVirtualThread(() -> process(ctx, msg))
        │
        ▼
      JmqttContext.getCtrl(msg.fixedHeader().messageType())
        │
        ▼
      BaseCtrl.processCtrl(ctx, msg)
        │
        ├── MqttVersion.protocolLevel() == 5 → Mqtt_5_xxx
        └── MqttVersion.protocolLevel() != 5 → Mqtt_3_4_xxx
        │
        ▼
      版本差异化处理（CONNECT 链式校验 / PUBLISH QoS 处理 / SUBSCRIBE 等）
        │
        ▼
      MessageDispatcher.accept(subscription, message)
```

### 5.2 出站（Broker → 客户端）

```
MessageDispatcher.DispatcherTask.run()
  │
  ├── 1. 批量从 messageQueue 取消息
  │
  ├── 2. SubscriptionMatcher.match(topic) → List<Subscription>
  │
  ├── 3. 对每个 subscription:
  │      ├── 计算 minQos = min(publisherQos, subscriberMaxQos)
  │      ├── MQTT 5: ReceiveMaximumManager.acquireOutbound()
  │      ├── QoS > 0: cacheOutboundMsg (sendCache)
  │      ├── message.copy() → 设置 QOS/msgId
  │      └── write(clientSession.channel, message)
  │
  └── 4. write 异常处理:
         ├── future 失败 → releaseOutbound + 可选 storeOffline
         └── channel inactive → releaseOutbound + storeOffline
```

### 5.3 QoS 2 状态机

**入站（Broker 收）**：
```
客户端 PUBLISH(DUP=0)
  → acquireInbound ✓
  → cacheInboundMsg (recCache)
  → 发 PUBREC

客户端 PUBLISH(DUP=1) 重发
  → existInboundMsg(recCache) = true
  → 只回 PUBREC，不重新 acquire

客户端 PUBREL
  → releaseInboundMsg (recCache.remove)
  → dispatcherMessage (下游投递)
  → releaseInbound
  → 发 PUBCOMP
```

**出站（Broker 发）**：
```
Broker PUBLISH(QoS=2)
  → acquireOutbound ✓
  → cacheOutboundMsg (sendCache)
  → 发 PUBLISH

客户端 PUBREC
  → releaseOutboundMsg (sendCache.remove)
  → cacheOutboundSecMsgId (secTwoCache)
  → 发 PUBREL

客户端 PUBCOMP
  → releaseOutboundSecMsgId (secTwoCache.remove)
  → releaseOutbound
```

---

## 6. ReceiveMaximum 配额管理（MQTT 5）

### 6.1 设计概述

MQTT 5.0 Receive Maximum 是**双向独立**的：
- **客户端 CONNECT Receive Maximum** → 服务端出站配额上限（服务端同时向该客户端发送 QoS > 0 消息数）
- **服务端 CONNACK Receive Maximum** → 服务端入站配额上限（服务端同时接受该客户端 QoS > 0 消息数）

### 6.2 类结构

```java
Quota (基类，纯 CAS 计数器)
├── clientId
├── receiveMaximum (配额上限)
├── quota (AtomicInteger)
├── acquire()    // CAS 自旋原子递减
├── release()    // CAS 自旋原子递增到上限
└── destroy()    // 空实现

OutboundQuota extends Quota (出站专属)
├── waitQueue (LinkedBlockingQueue<QuotaMsg>, 容量 1024)
├── waiting (AtomicBoolean, consumer 运行标志)
├── acquire(QuotaMsg)  // 失败时入等待队列
├── consumer()         // 时间轮驱动，waitQueue → send()
└── release()          // super.release() 后触发 consumer

ReceiveMaximumManager (静态入口)
├── OUT_FLOW_MAP: Map<String, OutboundQuota>
├── IN_FLOW_MAP: Map<String, Quota>
├── createOutbound(clientId, receiveMaximum)
├── destroyOutbound(clientId)
├── acquireOutbound(clientId)        // 无参，纯获取配额
├── acquireOutbound(clientId, sub, msg)  // 带消息，失败入队
├── releaseOutbound(clientId)
├── createInbound(clientId, receiveMaximum)
├── destroyInbound(clientId)
├── acquireInbound(clientId)
└── releaseInbound(clientId)
```

### 6.3 核心调用点

| 方向 | acquire 位置 | release 位置 |
|------|-------------|-------------|
| 出站 | `MessageDispatcher.send()` 内，`qos > 0 && mqtt5` 时 | `PubAckCtrl` / `PubCompCtrl` / write future / channel inactive / 消息过期 |
| 入站 | `Mqtt_5_message.handleQos1/2` 内，existInboundMsg 检查之后 | `handleQos1` 末尾 / `PubRelCtrl`（QoS 2） |

### 6.4 CAS 原子性保证

```java
// acquire: get + compareAndSet 自旋
while (true) {
    int current = quota.get();
    if (current <= 0) return false;
    if (quota.compareAndSet(current, current - 1)) return true;
}

// release: get + compareAndSet 自旋 + 上限守卫
while (true) {
    int current = quota.get();
    if (current >= receiveMaximum) return;
    if (quota.compareAndSet(current, current + 1)) break;
}
```

---

## 7. 定时调度模块（timer）

### 7.1 设计

- **底层**：Netty `HashedWheelTimer`（tick=150ms, wheelSize=512）
- **持久化**：schedule_task 表（timerId 主键，expireAt 过期时间，data 业务数据，cycle 周期标志）
- **任务恢复**：Broker 启动时从 DB 加载未过期任务重新入时间轮

### 7.2 API

```java
// 创建一次性延迟任务（5 秒后执行）
TimerUtil.add("session:expire:client123", TimerType.SESSION, 5, "client123");

// 创建周期任务（每 60 秒）
TimerUtil.addCycle("broker:health", TimerType.HEALTH_CHECK, 60, null);

// 取消 / 重调度
TimerUtil.cancel("session:expire:client123");
TimerUtil.reschedule("session:expire:client123", 3600);
TimerUtil.exists("session:expire:client123");
```

### 7.3 TimerType 枚举

| 类型 | 业务场景 |
|------|----------|
| SESSION | 会话过期（MQTT 5 Session Expiry Interval） |
| WILL | 遗嘱消息延迟发送（Will Delay Interval） |
| DELAY_PUB | 延迟消息发布（MQTT 5） |

### 7.4 表结构

```sql
CREATE TABLE schedule_task (
    timer_id   VARCHAR(255) PRIMARY KEY,
    type       VARCHAR(32) NOT NULL,       -- TimerType.name()
    expire_at  BIGINT NOT NULL,            -- 过期时间戳（秒）
    expire     BIGINT NOT NULL,            -- 过期间隔（秒），用于周期任务
    data       TEXT,                        -- 业务数据（JSON 或纯字符串）
    cycle      TINYINT DEFAULT 0,           -- 0=一次性, 1=周期
    exec       TINYINT DEFAULT 0            -- reload 时已过期任务处理: 0=丢弃, 1=立即执行
);
```

---

## 8. 集群模块

### 8.1 架构

```
┌───────────────────────┐     Hazelcast Queue      ┌───────────────────────┐
│       Node A           │◄──────────────────────► │       Node B           │
│  ┌─────────────────┐  │   ClusterMessage 收发    │  ┌─────────────────┐  │
│  │ ClusterMessage  │  │                          │  │ ClusterMessage  │  │
│  │  Dispatcher     │  │                          │  │  Dispatcher     │  │
│  └────────┬────────┘  │                          │  └────────┬────────┘  │
│           │            │                          │           │            │
│  ┌────────┴────────┐  │                          │  ┌────────┴────────┐  │
│  │  MessageHandler │  │  Publish / Subscribe     │  │  MessageHandler │  │
│  │  SessionHandler │  │  Session / Retain        │  │  SessionHandler │  │
│  │  RetainHandler  │  │                          │  │  RetainHandler  │  │
│  └─────────────────┘  │                          │  └─────────────────┘  │
└───────────────────────┘                          └───────────────────────┘
```

### 8.2 消息类型（ClusterMessage.EventType）

| 类型 | 数据 Record | 说明 |
|------|------------|------|
| PUBLISH | ClusterMessageRecord | 跨节点 PUBLISH |
| SUBSCRIBE | ClusterMessageRecord | 订阅关系同步 |
| UNSUBSCRIBE | ClusterMessageRecord | 取消订阅同步 |
| SESSION_CONNECT | ClusterSessionRecord | 客户端连接（节点广播通知其他节点） |
| SESSION_DISCONNECT | ClusterSessionRecord | 客户端断开 |
| RETAIN | RetainMessageRecord | 保留消息跨节点同步 |

### 8.3 Handler 注册表

每个 Node 启动时向 ClusterMessageDispatcher 注册 6 个 Handler：
- `PublishMessageHandler`
- `SubscriptionHandler`
- `UnSubscriptionHandler`
- `SessionConnectHandler`
- `SessionDisconnectHandler`
- `RetainMessageHandler`

### 8.4 Unicast 消费线程批量设计

#### 8.4.1 整体结构

```java
private final ExecutorService clusterMessageExecutor = Executors.newSingleThreadExecutor();
private volatile Thread pollThread;
```

- **pollThread**（虚拟线程）：负责从 Hazelcast `IQueue` 拉取 + 聚合批次
- **clusterMessageExecutor**（单线程）：负责执行批次内的 Handler，**保证同一节点 unicast 消息严格顺序**

> ⚠️ 必须单线程执行器，不能用 `newVirtualThreadPerTaskExecutor()`
> 因为第一批/第二批是独立 submit，无界虚拟线程池可能导致乱序（MQTT 集群控制消息有顺序依赖）

#### 8.4.2 核心流程（take + drainTo + poll(1s) 分裂批）

```
loop（pollThread）:
  ① unicastQueue.take()
     阻塞拿第一条，0 延迟启动，保证不丢消息

  ② firstBatch.add(msg) + drainTo(15)
     非阻塞捞队列已积压的，0 延迟
     → firstBatch 最终 size：1~16

  ③ submit(firstBatch) → clusterMessageExecutor
     ✅ 第一批消息**立即提交处理**（0 延迟）
     ⚠️ poll(1s) 在 submit 之后，完全不影响第一批！

  ④ unicastQueue.poll(1000, MILLISECONDS)
     带超时阻塞，触发第二批的"窗口"
     - 队列还有消息？立即返回（0 延迟触发第二批）
     - 队列空了？最多等 1s，有消息就触发，没就返回 null

  ⑤ 第二批处理：
     batch.add(next) + for 循环无参 poll() 捞剩余
     → submit(batch) → clusterMessageExecutor
     ⚠️ 第二批的 poll() 是无参非阻塞的，队空立即返回 null，不会卡死

  → 回到 ①（take）
```

#### 8.4.3 三负载场景表现

| 场景 | 延迟表现 | 批量行为 |
|------|----------|----------|
| **高负载**（队列积压 ≥16 条） | ✅ 0ms | take+drainTo 拿满 16 → poll(1s) 立即返回 → 第二批捞满 → 循环直到队列空，**瞬间拆多批满 16** |
| **中负载**（每秒均匀 5-10 条） | ✅ ≤1s（自然触发） | take 拿第一条 → drainTo 捞 0 → poll(1s) 窗口内陆续到达的消息自然触发第二批，聚合 5~16 条 |
| **低负载**（每秒仅 1 条） | ✅ 0ms（实际消息无延迟） | take 拿 1 条 → submit → poll(1s) 超时空转 → 循环回到 take。**1s 空转是消费者等待，不是消息延迟**（本来就没新消息要处理） |

#### 8.4.4 设计巧思

poll(1s) 放在**第一批 submit 之后**，这个位置是整个方案的核心：

| poll 位置 | 效果 |
|-----------|------|
| 在 take 之前 | ❌ 消息到了先等 poll 超时，硬延迟 |
| **submit 之后（本方案）** | ✅ 第一批 0 延迟处理；poll 窗口在第一批处理的同时去等第二批触发 |
| poll 无超时 | ❌ 队列空了跳过第二批，失去聚合能力 |
| **poll(1s) 带超时（本方案）** | ✅ 有消息立即返回（0 延迟触发）；没消息最多等 1s 就放弃（空转不影响） |

#### 8.4.5 已修复的 Bug 清单

| Bug | 位置 | 修复 |
|-----|------|------|
| forEach 内错误引用外层 `msg` 变量，导致所有消息用同一个 msg 处理 | 原 L79-82, L95-98 | 改为 lambda 参数 `m` |
| handler 为 null 时 NPE | 原 L80, L96 | 加 `if (handler != null)` 判断 |
| `catch(Throwable) → break` 导致整个消费者永久退出 | 原 L108-110 | 改为 `catch(Exception)` + `log.warn` + continue |
| 执行器无界导致消息乱序 | 原 `newVirtualThreadPerTaskExecutor()` | 改为 `newSingleThreadExecutor()` |
| stop() 未关闭线程池、未 interrupt poll 线程 | 原 stop() | 补全优雅关闭顺序 |

#### 8.4.6 魔法数字

| 常量 | 值 | 说明 |
|------|-----|------|
| BATCH_SIZE | 16 | 单批次最大消息数，平衡 batching 收益和内存占用 |
| NEXT_BATCH_WAIT_MS | 1000 | 第二批触发窗口，中负载聚合用。1s 是经过验证的合理值——MQTT 集群消息对 1s 以内的延迟完全不敏感 |

#### 8.4.7 Shutdown 顺序

```
stop() 执行顺序:
  1. interrupt pollThread      ← 先停拉取，避免继续 submit
  2. remove broadcast listener  ← 移除广播监听，避免重复处理
  3. executor.shutdown() + awaitTermination(5s)  ← 等已提交的批次处理完
  4. handlers.clear()          ← 最后清 handler，避免竞态
  5. unicastQueue.destroy()    ← 销毁分布式队列（try-catch 兜底）
```

---

## 9. 存储架构

### 9.1 存储分层

```
请求路径: Client → Netty → Ctrl → Store
                                    │
                         ┌──────────┴──────────┐
                         ▼                      ▼
                  LocalSessionStore       LocalMessageStore
                         │                      │
                   ┌─────┴─────┐           ┌────┴─────┐
                   ▼           ▼           ▼          ▼
              ConcurrentHashMap  H2      ConcurrentHashMap  H2
              (QoS 过程消息)    (会话)   (保留消息)        (遗嘱消息)
```

### 9.2 数据存储策略

| 数据 | 内存缓存 | H2 持久化 | 创建时机 | 清理时机 |
|------|----------|----------|----------|----------|
| 会话状态 | SessionState | Session (PO) | CONNECT | DISCONNECT / Session Expiry |
| 订阅关系 | SubscriptionMatcher 树 | SubscriptionDO | SUBSCRIBE | UNSUBSCRIBE / 会话过期 |
| QoS 1/2 Inflow | recCache | ❌ | PUBREC 后 | PUBREL 后 |
| QoS 1 Outflow | sendCache | ❌ | PUBLISH 发出 | PUBACK 后 |
| QoS 2 Outflow | sendCache + secTwoCache | ❌ | PUBLISH 发出 / PUBREL 发出 | PUBREC 后 / PUBCOMP 后 |
| 离线消息 | ❌ | OfflineMessage | PUBLISH 时客户端离线 | 客户端重连上线 |
| 遗嘱消息 | LocalMessageStore.willCache | WillMessage | CONNECT 时 | DISCONNECT / 遗嘱触发 |
| 保留消息 | LocalMessageStore.retainCache | RetainMessage | PUBLISH retain=1 | 同 topic 覆盖 / 遗嘱触发 |
| 定时任务 | ❌（时间轮内存） | ScheduleTask | TimerUtil.add | cancel / 执行完成 |

### 9.3 H2 连接

```
jdbc:h2:file:/opt/jmqtt/jmqtt;AUTO_SERVER=true;MODE=MYSQL
```

- `AUTO_SERVER=true`：多进程共享文件模式（集群时各节点独立文件，Hazelcast 负责同步）
- `MODE=MYSQL`：兼容 MySQL 语法
- ⚠️ 开发环境需手动创建 `/opt/jmqtt/` 目录，或修改 `LocalDB.java` 中的路径

---

## 10. 主题匹配

### 10.1 DefaultSubscriptionTreeMatcher

使用多叉树（Topic Tree）存储订阅关系：

```
root
├── $SYS/...
├── $share/
│   ├── groupA/topic/a
│   └── groupB/topic/b
├── test/
│   ├── +/
│   │   └── #
│   └── foo/bar
└── ...
```

**Token 规则**：
- `+` 匹配单层任意 token
- `#` 匹配后续所有 token（必须在末尾）
- `$share/group/topic` 共享订阅，匹配后按 group 名称分组
- `$SYS/` 系统主题，`$FILE/` 文件主题作为特殊前缀

### 10.2 匹配策略

1. 遍历路径：逐层 split `/` → 对每个 node 匹配 exact token / `+` / `#`
2. 共享订阅：同一 group 多个订阅者，**随机选一个**（策略可扩展）
3. 返回 `List<Subscription>`，支持一个 topic 匹配多个订阅者

---

## 11. Virtual Thread 使用模式

项目全链路使用 Java 21 Virtual Thread：

### 模式 1：单任务 VirtualThread

```java
Thread.startVirtualThread(() -> {
    JmqttContext.getCtrl(msg.fixedHeader().messageType())
        .processCtrl(ctx, msg);
});
```

**使用位置**：`NettyMqttHandler.channelRead()` — 每个 MQTT 报文用独立 VT 处理。

### 模式 2：VirtualThread Executor

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(new AsyncDispatcher(batch));
```

**使用位置**：`MessageDispatcher` — 批量消息分发。

### 注意事项

- Virtual Thread 做 IO 操作（H2 同步调用）会阻塞 Carrier Thread，但 H2 内存操作很快
- `newVirtualThreadPerTaskExecutor()` 自带线程池复用，适合 IO 密集型
- 避免长时间 CPU 密集型任务在 VT 上执行

---

## 12. 异常处理规范

### 12.1 异常类型

| 类型 | 场景 | 处理 |
|------|------|------|
| `BrokerException` | Broker 内部逻辑异常 | `BrokerException.of(Reason.XXX)` → 回复 MQTT 原因码 |
| `ConnectException` | 连接校验失败 | `ConnectException.of(MqttConnectReturnCode.XXX)` → CONNACK 拒绝 |
| `ConfigException` | YAML 配置缺失/非法 | 启动时抛出，阻止启动 |

### 12.2 兜底原则

- Ctrl 层 catch 所有异常，回复 MQTT 错误报文（不允许异常向上抛导致 channel 关闭）
- Hazelcast 分布式对象操作必须 try-catch（集群节点关闭时会抛 HazelcastInstanceNotActiveException）
- QoS 过程消息处理中，**网络发送失败不影响业务状态变更**（cache/release 先于 write）

---

## 13. 待实现清单

| 序号 | 功能 | 模块 | 状态 |
|------|------|------|------|
| 1 | Session Expiry 定时器 | timer | TimerUtil 已有，业务集成待做 |
| 2 | Will Delay 定时发送 | timer | TimerUtil.WILL handler 已有，业务集成待做 |
| 3 | 集群消息转发完整链路 | cluster | Handler 已注册，转发逻辑部分完成 |
| 4 | BrokerConfig 全量暴露到 jmqtt.yml | config | 大量参数仍硬编码 Java 默认值 |
| 5 | Session 缓存内存条目主动清理 | store | 依赖会话过期 + 孤儿兜底扫描 |
| 6 | 测试用例 | 全模块 | 无 src/test 目录 |
