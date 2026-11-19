# 并发编程规范

> 适用范围：JMQTT Broker 全部多线程、异步、CAS、VirtualThread 相关代码

---

## 1. 并发工具选择指南

### 1.1 何时用什么

| 场景 | 推荐 | 不要用 |
|------|------|--------|
| 原子计数器 / 标志位 | `AtomicInteger` / `AtomicBoolean` | `volatile` + 普通变量 |
| 原子 CAS 状态转换 | `AtomicReference` + CAS 自旋 | `synchronized` |
| 并发 Map / Set | `ConcurrentHashMap` / `ConcurrentHashSet` | `Collections.synchronizedMap` |
| 并发队列 | `LinkedBlockingQueue` / `ArrayBlockingQueue` | `ArrayList` + `synchronized` |
| 可重入锁 | `ReentrantLock` | `synchronized`（除非语义要求 monitor） |
| 线程安全延迟初始化 | `AtomicBoolean` + CAS | 双重检查锁（易写错） |
| 多线程等待 | `CountDownLatch` / `CompletableFuture` | 手动 `wait()/notify()` |

### 1.2 项目禁用清单

| 禁用 | 原因 |
|------|------|
| `synchronized` 关键字 | 容易写出死锁、锁范围过大；项目用 CAS/Concurrent 集合替代 |
| `Thread.sleep()` 在业务逻辑中 | 阻塞线程且无超时控制 |
| `Future.get()` 无限等待 | 必须带超时 |
| `ThreadLocal` | Virtual Thread 环境下容易内存泄漏 |

---

## 2. CAS 自旋模式

### 2.1 标准模式

```java
private final AtomicInteger counter = new AtomicInteger(initial);

public boolean tryAcquire() {
    while (true) {
        int current = counter.get();
        if (current <= 0) return false;          // 不满足条件，快速返回
        if (counter.compareAndSet(current, current - 1)) return true;  // CAS 成功
        // CAS 失败，循环重试
    }
}
```

**关键点**：
- `get()` + `compareAndSet()` 是原子序列，但 CAS 失败说明中间被其他线程修改了
- 不满足前置条件时**立即返回**，不要 CAS 一个无效值
- 自旋次数通常 1-2 次就成功，极端冲突场景可能需要退避

### 2.2 带上限的 CAS

```java
public void release() {
    while (true) {
        int current = quota.get();
        if (current >= receiveMaximum) return;    // 已达上限，不递增
        if (quota.compareAndSet(current, current + 1)) break;
    }
}
```

### 2.3 危险的非原子写法

```java
// ❌ 致命：两步非原子操作，并发时可能 decrement 到负数
public boolean acquire() {
    if (quota.intValue() == 0) return false;   // 步骤 1：检查
    return quota.decrementAndGet() >= 0;      // 步骤 2：递减 —— 并发时可能在 1→2 之间被抢占
}

// ❌ 同样致命
public void release() {
    if (quota.intValue() < receiveMaximum) {   // 检查
        quota.incrementAndGet();                // 递增 —— 并发时可能溢出上限
    }
}
```

**修复**：用上面的 CAS 自旋替换。

---

## 3. Virtual Thread 使用规范

### 3.1 创建方式

```java
// ✅ 方式 1：单任务，即时执行
Thread.startVirtualThread(() -> {
    JmqttContext.getCtrl(type).processCtrl(ctx, msg);
});

// ✅ 方式 2：Executor，批量 IO 密集任务
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(new DispatcherTask(batch));

// ❌ 不要用 Executors.newFixedThreadPool —— 用 VT Executor 代替
```

### 3.2 适用场景

**适合 VT**：
- Netty handler 层每报文一个 VT（IO 密集 + 短生命周期）
- 消息分发、DB 查询（H2 内存操作）
- 定时任务 dispatch

**不适合 VT**：
- 长时间 CPU 密集型计算
- 批量 H2 写操作（如果未来换为 MySQL/PostgreSQL 远程 DB，连接池需要调整）
- 原生库调用（JNI）

### 3.3 关闭时机

```java
// ✅ 好：Broker shutdown 时统一关闭
public void shutdown() {
    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);
}
```

### 3.4 注意事项

| 项 | 说明 |
|----|------|
| Carrier Thread 阻塞 | VT 做同步 IO 会阻塞 Carrier，但 H2 内存操作很快，可接受 |
| 栈深度 | VT 默认栈只有几 KB，避免深层递归 |
| ThreadLocal | VT 数量大（百万级）时 ThreadLocal 会导致内存泄漏 |
| Netty 兼容性 | Netty 4.1.x 对 VT 支持良好（channel 操作会自动切回 NIO） |

---

## 4. ConcurrentHashMap 操作规范

### 4.1 get → putIfAbsent 模式

```java
// ✅ 好：原子性的获取或创建
Quota quota = OUT_FLOW_MAP.get(clientId);
if (quota == null) {
    quota = new OutboundQuota(clientId, receiveMaximum);
    Quota existing = OUT_FLOW_MAP.putIfAbsent(clientId, quota);
    if (existing != null) {
        quota.destroy();  // CAS 失败，回收多余对象
        quota = existing;
    }
}
```

### 4.2 遍历期间修改

`ConcurrentHashMap.entrySet()` 返回的是弱一致迭代器，允许遍历期间修改，但迭代到的元素可能不一致。

```java
// ✅ 好：遍历做只读操作
for (Map.Entry<String, ConcurrentHashMap<Integer, Message>> entry : inboundCache.entrySet()) {
    // 读 entry.getKey() / entry.getValue() 安全
}

// ⚠️ 注意：遍历时 remove 需要用迭代器
for (Iterator<Map.Entry<String, ConcurrentHashMap<Integer, Message>>> it = inboundCache.entrySet().iterator(); it.hasNext(); ) {
    Map.Entry<String, ConcurrentHashMap<Integer, Message>> entry = it.next();
    if (entry.getValue().isEmpty()) {
        it.remove();  // 迭代器 remove 安全
    }
}
```

### 4.3 不要用 computeIfAbsent 缓存重对象

```java
// ❌ 坏：computeIfAbsent 会在 Map 加锁期间执行 lambda
// 如果 lambda 中有耗时操作（如 DB 查询），会阻塞整个 Map
sessionStore.computeIfAbsent(clientId, id -> loadFromDB(id));

// ✅ 好：get → putIfAbsent 两步，lambda 不持锁
SessionState state = sessionStore.get(clientId);
if (state == null) {
    SessionState loaded = loadFromDB(clientId);
    state = sessionStore.putIfAbsent(clientId, loaded);
}
```

---

## 5. 时间轮定时任务

### 5.1 HashedWheelTimer 配置

```java
new HashedWheelTimer(r -> {
    Thread t = new Thread(r, "jmqtt-schedule-wheel");
    t.setDaemon(true);
    return t;
}, 150, TimeUnit.MILLISECONDS, 512);
// tick=150ms: 精度 150ms，足够 MQTT 场景
// wheelSize=512: 时间轮桶数，任务量千级足够
```

### 5.2 取消必须原子化

```java
// ✅ 好：cancel 时间轮 + 删 DB + 清内存 同步执行
public void cancel(String timerId) {
    Timeout timeout = timeouts.remove(timerId);
    if (timeout != null) {
        timeout.cancel();
        storage.remove(timerId);  // DB 也要删，防止重启恢复
    }
}
```

### 5.3 周期任务

```java
// 周期任务触发后，重新 schedule 下一次
private void schedule(ScheduleTask task, long delayMs) {
    TimerTask timerTask = timeout -> {
        dispatchTask(task);
        if (task.getCycle() == 1) {
            // 更新 expireAt + DB + 重新 schedule
            task.setExpireAt(System.currentTimeMillis() / 1000L + task.getExpire());
            storage.save(task);
            schedule(task, task.getExpire() * 1000L);
        } else {
            storage.remove(task.getTimerId());  // 一次性任务，执行完删
        }
    };
    Timeout timeout = wheelTimer.newTimeout(timerTask, delayMs, TimeUnit.MILLISECONDS);
    timeouts.put(task.getTimerId(), timeout);
}
```

---

## 6. Shutdown 顺序规范

### 6.1 必须禁用框架自动 Hook

```java
// ✅ 好：禁用 Hazelcast 自动 shutdown hook
System.setProperty("hazelcast.shutdownhook.enabled", "false");
// ✅ 好：自己控制关闭顺序
JmqttCluster.shutdown();
```

### 6.2 关闭顺序

```
1. 停止接收新连接（Netty Server close）
2. 停止定时任务（TimerUtil.stop → scheduleTimer.stop）
3. 停止消息分发（MessageDispatcher executor shutdown）
4. 停止集群（ClusterMessageDispatcher.stop → consumer 中断 → Handler 清理）
5. 停止 Hazelcast（CLUSTER.shutdown()，try-catch 兜底）
6. 关闭 Netty channel（Netty shutdownGracefully）
7. 关闭 DB（LocalDB shutdown）
```

### 6.3 并发关闭保护

```java
private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

public static void shutdown() {
    if (!SHUTTING_DOWN.compareAndSet(false, true)) {
        return;  // 防止并发/重复调用
    }
    // ... 关闭逻辑
}
```

### 6.4 Hazelcast 分布式操作兜底

**所有 Hazelcast 分布式对象操作必须 try-catch**：

```java
// ✅ 好：catch Throwable 兜底
try {
    unicastQueue.destroy();
} catch (Throwable ignored) {
    // HazelcastInstanceNotActiveException 在 shutdown 时正常出现
}

// ❌ 坏：不 catch
unicastQueue.destroy();  // shutdown 时 Hazelcast 已关闭，抛异常导致线程崩溃
```

**catch 范围**：用 `Throwable` 而非 `Exception`，因为 `HazelcastInstanceNotActiveException` 继承自 `RuntimeException`，在 shutdown hook 线程上会被包装为 AssertionError。

---

## 7. 常见陷阱 Checklist

### 7.1 CAS 相关

- [ ] acquire / release 是两步操作还是一步？（一步 CAS 自旋才安全）
- [ ] CAS 失败后有没有重试？
- [ ] CAS 条件检查在 CAS 之前还是之后？（必须在之前快速返回）

### 7.2 ConcurrentHashMap 相关

- [ ] 外层 Map 的 entry 有没有在合适时机 remove？（防止 clientId 永久累积）
- [ ] 用了 computeIfAbsent 吗？（lambda 不能有耗时操作）
- [ ] 遍历修改用迭代器 remove 吗？

### 7.3 线程安全

- [ ] Virtual Thread 上有没有 ThreadLocal？
- [ ] shutdown hook 是自己注册还是框架自动？
- [ ] Hazelcast destroy 有没有 try-catch？
- [ ] AtomicBoolean/CAS 单例关闭标志是否防止了并发调用？

### 7.4 资源管理

- [ ] Timeout / Future / executor 有没有 cancel / shutdown？
- [ ] DB 连接用的是 try-with-resources 还是手动管理？
- [ ] 定时任务 dispatch 异常有没有 catch？（不能让时间轮线程崩溃）
