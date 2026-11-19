# AGENTS.md — JMQTT 协作指南

> 本文档面向 AI Agent（IDE 助手、代码生成器）和人类协作者。定义**禁止行为清单**、**编码红线**和**修改后必须执行的检查项**。
>
> 读完后你应该清楚：**什么绝对不能做**、**改代码时哪些不变量必须保持**、**提交前要对哪些 Checklist 打勾**。

---

## 1. 技术栈（速查）

| 组件 | 版本 | 备注 |
|------|------|------|
| Java | 21 | Virtual Thread 语法、record、pattern matching 可用 |
| Netty | 4.1.54.Final | MQTT Codec + HashedWheelTimer |
| Hazelcast | 5.7.0 | 集群通信；**必须**禁用自动 shutdown hook |
| MyBatis-Plus | 3.5.17 | **必须**排除 mybatis-plus-spring |
| H2 | 2.5.252 | 文件模式 `AUTO_SERVER=true`，MySQL 兼容模式 |
| Hutool | 5.8.46 | 工具库 |
| Caffeine | 2.9.3 | 本地缓存 |
| SnakeYAML | 2.7 | 配置解析 |
| Lombok | 1.18.40 | 简化代码 |

---

## 2. 绝对禁止行为

### 2.1 框架依赖

| ❌ 禁止 | 原因 |
|---------|------|
| 引入 Spring / Spring Boot 依赖 | 项目定位是"零 Spring"；pom 中 mybatis-plus 已 exclusion 掉 spring 模块 |
| 使用 `synchronized` 关键字 | 容易死锁、锁范围难以精确；用 CAS / Concurrent 集合 / ReentrantLock |
| 使用 `ThreadLocal` | Virtual Thread 数量大时易内存泄漏 |
| 在业务代码中 `Thread.sleep()` | 阻塞线程且无超时控制；用 HashedWheelTimer 定时任务 |

### 2.2 日志 & 异常

| ❌ 禁止 | 替换 |
|---------|------|
| `System.out.println` / `System.err.println` | `log.info/warn/error`（Lombok @Slf4j） |
| `printStackTrace()` | 统一 `log.error("msg", e)` |
| 吞异常 | 至少 `log.warn/error`，或向上抛；**禁止**空 `catch (Exception e) {}` |
| 跨层抛 RuntimeException | 业务异常用 `BrokerException.of(Reason.XXX)`；连接异常用 `ConnectException.of(MqttConnectReturnCode.XXX)` |

### 2.3 MQTT 协议语义红线

| ❌ 禁止 | 原因 |
|---------|------|
| cache/release 状态绑死 `future.success` | 网络回执失败时业务状态丢失；**cache 必须先于 write** |
| acquire/release 配额不配对 | QoS 1/2 配额泄漏静默累积，最终 Receive Maximum 耗尽 |
| 两步非原子 acquire | `if (count > 0) count--` 并发时可能变负数；**必须 CAS 自旋** |
| 重发 PUBLISH(QoS 2) 重复 acquire | existInboundMsg 前置拦截，重发只回 PUBREC，不重复配额 |
| QoS 0 消息走配额 acquire | QoS 0 不等待回执，永远不会 release |
| ConcurrentHashMap 外层 entry 不清理 | `Map<clientId, Map<msgId, ...>>` 中 clientId 外层 entry 永久累积 |

### 2.4 Hazelcast 红线

| ❌ 禁止 | 原因 |
|---------|------|
| 依赖 Hazelcast 自动 shutdown hook | 5.x 在 shutdown 期间仍投递消息，consumer 会崩溃 |
| Hazelcast 分布式操作不 try-catch | shutdown 期间抛 `HazelcastInstanceNotActiveException` 导致线程崩溃 |
| catch(Exception) | Hazelcast 在 shutdown hook 上抛的是 `AssertionError`；**用 `catch(Throwable)`** |

### 2.5 其他

| ❌ 禁止 | 原因 |
|---------|------|
| 在 `ConcurrentHashMap.computeIfAbsent` lambda 中做慢操作 | lambda 执行期间 Map 持锁，阻塞所有其他操作 |
| Hazelcast 日志不桥接 SLF4J | 启动时刷几百行 INFO |
| 硬编码网络 IP / 端口 / 路径 | 走 yml 配置；H2 路径例外（见 LocalDB.java） |

---

## 3. 编码红线（必须遵守）

### 3.1 CAS 原子操作

```java
// ✅ 唯一正确写法
public boolean acquire() {
    while (true) {
        int current = quota.get();
        if (current <= 0) return false;
        if (quota.compareAndSet(current, current - 1)) return true;
    }
}

public void release() {
    while (true) {
        int current = quota.get();
        if (current >= receiveMaximum) return;
        if (quota.compareAndSet(current, current + 1)) break;
    }
}
```

### 3.2 Virtual Thread 启动

```java
// ✅ 每报文一个 VT
Thread.startVirtualThread(() -> ctrlTable.get(type).processCtrl(ctx, msg));

// ✅ 批量分发用 VT Executor
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(new DispatcherTask(batch));
```

### 3.3 Hazelcast 分布式操作兜底

```java
// ✅ 必须 try-catch Throwable
try {
    unicastQueue.destroy();
} catch (Throwable ignored) {
    // shutdown 期间正常出现 HazelcastInstanceNotActiveException
}
```

### 3.4 SessionStore 缓存清理

```java
// ✅ 外层 entry 一并 remove
public Message releaseOutboundMsg(String clientId, Integer msgId) {
    ConcurrentHashMap<Integer, Message> v = outboundCache.get(clientId);
    if (msgId == null) {
        if (v != null) v.clear();
        outboundCache.remove(clientId);   // ← 关键
        return null;
    }
    return v != null ? v.remove(msgId) : null;
}
```

### 3.5 日志级别选择

| 级别 | 场景 |
|------|------|
| `log.error` | 异常捕获、启动/关闭失败、资源泄漏 |
| `log.warn` | 非预期但可恢复（重发、配置降级、配额不足） |
| `log.info` | 关键状态（启动完成、端口监听、集群 join、连接建立/断开） |
| `log.debug` | 高频明细（每报文内容、匹配结果） |

---

## 4. 命名约定

| 类型 | 格式 | 示例 |
|------|------|------|
| 版本差异化 Handler | `Mqtt_{protocol}_{action}` | `Mqtt_3_4_message` / `Mqtt_5_connect` |
| 报文处理器 | `{MqttType}Ctrl` | `ConnectCtrl` / `PubRecCtrl` |
| Handler 接口 | `{Action}Handler` | `MessageHandler` / `SubscriptionHandler` |
| Store 接口 | `{Entity}Store` | `SessionStore` / `MessageStore` |
| Store 单机实现 | `Local{Entity}Store` | `LocalSessionStore` |
| PO（数据库实体） | `{Entity}` | `Session` / `ScheduleTask` |
| Mapper | `{Entity}Mapper` | `SessionMapper` / `ScheduleTaskMapper` |
| DTO / Record | `{Action}Record` | `ClusterMessageRecord` / `ClusterSessionRecord` |
| 工具类 | `{Name}Util` / `{Name}Helper` | `MessageUtil` / `ChannelHelper` |
| 枚举 | `{Name}` | `Reason` / `TimerType` / `SessionState` |

---

## 5. 目录结构速查

```
jmqtt-core/src/main/java/com/jmqtt/core/
├── JmqttBroker.java               ⭐ 启动入口
├── JmqttContext.java              ⭐ 静态上下文（所有单例从这里拿）
├── cluster/                       Hazelcast 集群
│   ├── JmqttCluster.java          启停 + broadcast
│   ├── ClusterConfig.java         seedNodes / port / nodeName
│   ├── ClusterMessageDispatcher   消息收发
│   ├── handler/                   集群消息处理器（6 个）
│   └── seria/                     Kryo 序列化
├── config/JmqttConfig.java        顶层配置对象
├── netty/                         网络层（TCP/WS/SSL 四端口）
│   ├── NettyServer.java
│   ├── session/ClientSession.java  单连接会话
│   └── event/ChannelEvent.java
└── mqtt/                          ⭐ 协议核心
    ├── helper/
    │   ├── Reason.java            MQTT v5 原因码（43 个）
    │   ├── MessageUtil.java       消息构建工具
    │   └── ReceiveMaximumManager  ⭐ 配额管理（CAS 自旋）
    ├── model/
    │   ├── Message.java           统一消息模型
    │   ├── MessageHeader.java     Header Key 常量
    │   └── Subscription.java
    ├── control/
    │   ├── BaseCtrl.java          版本分发基类
    │   ├── PublishCtrl.java       等 10 个 Ctrl
    │   ├── handler/               Mqtt_3_4_xxx / Mqtt_5_xxx
    │   ├── dispatcher/MessageDispatcher.java
    │   └── resend/ReSendMessageService.java
    ├── store/
    │   ├── SessionStore.java      ⭐ 会话存储接口
    │   ├── LocalSessionStore.java   内存 + H2 实现
    │   ├── MessageStore.java      ⭐ 遗嘱/保留消息
    │   └── persist/               LocalDB + Mapper + PO
    ├── subscribe/
    │   └── DefaultSubscriptionTreeMatcher.java  ⭐ 主题树
    └── timer/
        ├── TimerUtil.java         ⭐ 业务调用入口
        ├── ScheduleTaskScheduler  HashedWheelTimer 核心
        └── LocalDBScheduleTaskStorage.java
```

---

## 6. 修改后必须打勾的 Checklist

### 6.1 改了 MQTT 报文处理（Ctrl / Handler）

- [ ] 版本分流正确：`Mqtt_3_4_xxx` 和 `Mqtt_5_xxx` 分别处理了吗？
- [ ] BaseCtrl.preCheck() 没漏掉调用？
- [ ] QoS 流程幂等吗？重发消息能正确拦截吗？
- [ ] ReceiveMaximum acquire/release 配对？
- [ ] cache 先于 write 了吗？
- [ ] 没引入 synchronized / Thread.sleep / System.out 吧？

### 6.2 改了 ReceiveMaximumManager / Store 缓存

- [ ] CAS 是原子操作吗？（没有两步非原子）
- [ ] acquire/release 所有退出路径都覆盖了吗？（5 出站 + 2 入站）
- [ ] ConcurrentHashMap 外层 Map entry 有清理吗？
- [ ] QoS 0 不消耗配额吧？

### 6.3 改了 Hazelcast 相关

- [ ] 分布式操作 try-catch Throwable 了吗？
- [ ] 没有依赖自动 shutdown hook？
- [ ] AtomicBoolean 并发关闭守卫加了吗？

### 6.4 改了 MessageDispatcher

- [ ] pollThread 在所有退出路径都重置了 waiting 标志？
- [ ] VT Executor shutdown 了吗？

### 6.5 改了定时调度 TimerUtil

- [ ] Timeout cancel 后 DB 记录也删了？
- [ ] 周期任务 expireAt 更新 + 重 schedule 了吗？
- [ ] 时间轮 dispatch 异常 try-catch 了吗？（不能让时间轮线程崩）

### 6.6 通用

- [ ] 没有引入 Spring 依赖？
- [ ] 没有用 synchronized 关键字？
- [ ] 日志用 logback，不是 System.out？
- [ ] 没有空 catch 吞异常？
- [ ] 新增的依赖在父 pom 的 dependencyManagement 里声明版本了吗？
- [ ] mybatis-plus 没误删 exclusion？
- [ ] 启动 / 关闭时序没被破坏？
- [ ] 改完 `mvn compile` 没报错？

---

## 7. 踩坑速查

| # | 症状 | 根因 | 修复 |
|---|------|------|------|
| 1 | Receive Maximum 逐渐耗尽不再恢复 | acquire 有路径漏掉 release | 核对 5+2 个 release 位置 |
| 2 | Hazelcast shutdown 抛 `HazelcastInstanceNotActiveException` | 分布式操作没 catch Throwable | 所有 Hazelcast 对象操作 try-catch Throwable |
| 3 | Hazelcast 启动刷几百行 INFO | 日志没桥接到 SLF4J | `System.setProperty("hazelcast.logging.type", "slf4j")` + logback 配置 `com.hazelcast=ERROR` |
| 4 | SessionStore 内存持续增长 | 外层 `clientId → Map` entry 没 remove | `release*(clientId, null)` 时 `outerMap.remove(clientId)` |
| 5 | CAS 计数器变负数 | 两步非原子 `if (count>0) count--` | 改成 CAS 自旋 |
| 6 | mybatis-plus SPI 加载失败 | 依赖了 mybatis-plus-spring | exclusion 排除 |
| 7 | PUBREL 重发导致异常 PUBCOMP 逻辑 | PubRelCtrl 应该**直接发 PUBCOMP**，不做 release | releaseInboundMsg 返回 null → 跳过 release，直接 PUBCOMP |
| 8 | Windows 上 H2 连接失败 | 硬编码路径 `/opt/jmqtt/jmqtt` | 改成本地相对路径 |
| 9 | JVM 关闭顺序随机崩溃 | 禁用了 Hazelcast auto hook 但自己的 shutdown 顺序错 | JmqttBroker.shutdown()：netty → dispatcher → timer → hazelcast |

---

## 8. 三个文档的分工

| 文档 | 读者 | 内容 |
|------|------|------|
| **AGENTS.md**（本文档） | AI / 协作者 | 禁止行为、编码红线、修改后 Checklist |
| PROJECT.md | AI / 新成员 | 调用链、热路径、风险点 |
| README.md | 所有人 | 启动命令、配置、常见报错 |

规范详情见 `specs/` 目录：
- `java-code-style.md` — 命名、日志、异常、Lombok 用法
- `mqtt-message-handling.md` — QoS 缓存、配额、幂等三层拦截
- `concurrency.md` — CAS 自旋、VT、ConcurrentHashMap、时间轮、Shutdown 保护
