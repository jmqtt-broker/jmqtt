# JMQTT

[![Java](https://img.shields.io/badge/Java-21%2B-blue)](https://openjdk.org/projects/jdk/21/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![MQTT](https://img.shields.io/badge/MQTT-3.1%20%2F%203.1.1%20%2F%205.0-red)](https://mqtt.org/)

> 去中心化分布式 MQTT Broker，Java 21 + Netty + Hazelcast，不依赖 Spring Framework。

---

## 功能特性

- ✅ MQTT 3.1 / 3.1.1 / 5.0 协议完整支持
- ✅ TCP、WebSocket、SSL/TCP、SSL/WebSocket 四种传输
- ✅ 基于 Hazelcast 的分布式集群（种子节点自动发现）
- ✅ 内置 H2 文件数据库持久化（无需外部 DB）
- ✅ QoS 0 / 1 / 2 完整消息流程（含幂等处理）
- ✅ MQTT 5 Receive Maximum 双向独立配额管理
- ✅ 遗嘱消息 (Will Message)、保留消息 (Retain Message)、离线消息
- ✅ 基于 Netty HashedWheelTimer 的定时调度模块
- ✅ 全链路 Virtual Thread（Java 21）
- ✅ 主题树订阅匹配（支持 `+`、`#`、`$share/` 共享订阅）

## 环境要求

| 依赖 | 最低版本 |
|------|---------|
| JDK | 21 |
| Maven | 3.8+ |
| 操作系统 | Linux / macOS / Windows |

## 快速开始

### 1. 构建

```bash
# 克隆项目
git clone https://github.com/zt422680855/jmqtt.git
cd jmqtt

# 编译打包（跳过测试）
mvn clean package -DskipTests

# 产物路径
# jmqtt-core/target/jmqtt-core-1.0.0.jar
```

### 2. 启动

**方式 A：使用 classpath + 主类**

```bash
java -cp jmqtt-core/target/jmqtt-core-1.0.0.jar \
     com.jmqtt.core.JmqttBroker
```

**方式 B：指定外部配置文件路径**

```bash
java -cp jmqtt-core/target/jmqtt-core-1.0.0.jar \
     com.jmqtt.core.JmqttBroker \
     --config=/path/to/jmqtt.yml
```

启动成功后会看到类似日志：
```
INFO  c.j.core.JmqttBroker - Jmqtt broker starting...
INFO  c.j.core.netty.NettyServer - Netty server started on port 1883
INFO  c.j.core.JmqttBroker - Jmqtt broker ready.
```

### 3. 验证

**用任何 MQTT 客户端连接测试**（默认账号 `jmqtt / jmqtt`）：

```bash
# 命令行方式（macOS / Linux，需安装 mosquitto 客户端）
mosquitto_sub -h 127.0.0.1 -p 1883 -t "test/topic" -u jmqtt -P jmqtt -v

# 另一个终端发布
mosquitto_pub -h 127.0.0.1 -p 1883 -t "test/topic" -m "hello jmqtt" -u jmqtt -P jmqtt
```

## 配置文件

配置文件位于 `jmqtt-core/src/main/resources/jmqtt.yml`，运行时可通过 `--config=xxx.yml` 指定外部路径。

```yaml
broker:
  anonymous: false          # 是否允许匿名连接
  user: jmqtt               # 用户名
  password: jmqtt           # 密码

netty:
  tcpPort: 1883             # TCP 端口
  websocketPort: 1884       # WebSocket 端口
  sslTcpPort: 8883          # SSL/TCP 端口
  sslWebsocketPort: 8884    # SSL/WebSocket 端口

cluster:
  enable: true              # 是否启用集群
  clusterName: "JMQTT-CLUSTER"
  port: 5701                # Hazelcast 通信端口
  seedNodes: ["127.0.0.1:5701"]  # 种子节点列表
```

## 集群部署

多节点集群（同一机器开多个 JVM 做演示）：

```bash
# 节点 A
java -cp jmqtt-core/target/jmqtt-core-1.0.0.jar \
     com.jmqtt.core.JmqttBroker \
     --config=node-a.yml   # node-a.yml 中 cluster.port=5701

# 节点 B
java -cp jmqtt-core/target/jmqtt-core-1.0.0.jar \
     com.jmqtt.core.JmqttBroker \
     --config=node-b.yml   # node-b.yml 中 cluster.port=5702, seedNodes=["127.0.0.1:5701"]
```

## 数据库

内置 H2 文件数据库，默认路径硬编码在 `LocalDB.java:29`：

```
jdbc:h2:file:/opt/jmqtt/jmqtt
```

**Windows 开发时需修改路径**，例如：

```java
private final String localStoreUrl = "jdbc:h2:file:./data/jmqtt;AUTO_SERVER=true;MODE=MYSQL;...";
```

启动时会自动执行 `resources/sql/jmqtt_local.sql` 初始化 8 张表。

## 常见报错

### `SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"`

**原因**：classpath 缺少 logback 实现包。
**解决**：`jmqtt-core/pom.xml` 中确保有 `logback-classic` 依赖（父 pom 已声明）。

### Hazelcast 启动时大量 INFO/WARN 日志刷屏

**解决**：`logback.xml` 中加一条 logger：

```xml
<logger name="com.hazelcast" level="ERROR"/>
```

### `HazelcastInstanceNotActiveException` — 关闭时抛异常

**原因**：JVM 关闭时 Hazelcast 自动 shutdown hook 与应用关闭顺序冲突。
**解决**：启动前设置 `System.setProperty("hazelcast.shutdownhook.enabled", "false")`，并在所有 Hazelcast 分布式对象操作处加 `try-catch Throwable`。

### `mybatis-plus-spring` SPI 加载失败

**解决**：`jmqtt-core/pom.xml` mybatis-plus 依赖中必须排除 spring 模块：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### `org.h2.jdbc.JdbcSQLNonTransientConnectionException` — 数据库连接被占用

**原因**：H2 文件模式 `AUTO_SERVER=true` 时，已有另一个 JVM 持有文件锁。
**解决**：确认没有其他 JMQTT 进程在使用同一数据库文件，或改路径。

## 目录结构

```
jmqtt/
├── pom.xml                          # 父 POM（依赖版本管理）
├── AGENTS.md                        # AI / 协作者指南
├── PROJECT.md                       # 项目全景文档
├── README.md                        # 本文档
└── jmqtt-core/                      # 核心模块（唯一子模块）
    ├── pom.xml
    └── src/main/
        ├── java/com/jmqtt/core/
        │   ├── JmqttBroker.java             # 启动入口
        │   ├── JmqttContext.java            # 全局静态上下文
        │   ├── cluster/                     # Hazelcast 集群
        │   ├── common/                      # 工具类
        │   ├── config/                      # 配置对象
        │   ├── exception/                   # 异常定义
        │   ├── netty/                       # Netty 网络层
        │   └── mqtt/                       # MQTT 协议核心
        │       ├── auth/
        │       ├── control/                 # 报文处理 Ctrl
        │       │   └── dispatcher/          # 消息分发器
        │       ├── helper/                  # ReceiveMaximumManager, Reason
        │       ├── model/                   # Message, Subscription
        │       ├── store/                   # SessionStore, MessageStore + persist/
        │       ├── subscribe/               # 主题树匹配
        │       └── timer/                   # 定时调度模块
        └── resources/
            ├── jmqtt.yml
            ├── logback.xml
            └── sql/jmqtt_local.sql
```

## 技术栈

| 组件 | 依赖 | 版本 |
|------|------|------|
| 语言 | OpenJDK | 21 |
| 网络框架 | Netty | 4.1.54.Final |
| 集群 | Hazelcast | 5.7.0 |
| 持久化 | MyBatis-Plus + H2 | 3.5.17 / 2.5.252 |
| 工具库 | Hutool | 5.8.46 |
| 缓存 | Caffeine | 2.9.3 |
| 序列化 | Kryo | 5.4.0 |
| 配置 | SnakeYAML | 2.7 |
| 日志 | SLF4J + Logback | 1.7.30 / 1.2.13 |
| 简化 | Lombok | 1.18.40 |

## License

MIT License — see [LICENSE](LICENSE) for details.
