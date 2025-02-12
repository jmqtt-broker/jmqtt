1.pom文件引入：

```plain
<dependency>
    <groupId>io.github.jmqtt-broker</groupId>
    <artifactId>jmqtt-springboot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

2.springboot配置文件application.yml中增加如下配置：

```yaml
 jmqtt:
      broker:
        # 是否开启匿名访问、默认登录账号、默认登录密码，可通过重写AuthValid来校验登录
        anonymous-enable: true
        # mqtt的登录账号和密码，默认admin/admin，可通过重写AuthValid来校验登录
        user: admin
        pwd: admin
        mqtt5:
          # 是否使用服务端心跳周期
          use-server-keepalive: false
          default-keepalive: 15
          # 服务端能同时处理的非qos0最大消息数，默认int最大值
          receive-maximum: 100
          # 服务端能处理的最大packet长度，默认256M，也是mqtt报文数据最大长度（可变头 + payload）
          maximum-packet-size: 20000
          # 主题别名最大值，默认65535
          topic-alias-maximum: 1024
          # 服务端生成的clientId前缀，当客户端未设置clientId时，由服务端生成clientId返回，mqtt5新增
          client-id-prefix: JMQTT_CLIENT_
          # 可选的服务端功能，通配符订阅、订阅标识符、共享订阅、保留消息、最大qos等级
          wildcard-subscription-available: true
          subscription-identifier-available: true
          shared-subscription-available: true
          retain-available: true
          maximum-qos: 2
        # 是否开启高性能模式，开启则使用内存缓存QOS1、2的过程消息
        high-performance: true
        # 数据存储仓库，默认mem为内存存储，可选redis、rdb，配置redis需要配置redis连接，配置rdb需要数据库连接
        store: mem
        # 默认值true
        # store = rdb条件下，useDefaultRdb为true时，优先使用依赖方数据源，依赖方没有数据源使用rdb配置的数据源
        # useDefaultRdb为false时，使用rdb配置的数据源
        # useDefaultRdb、rdb配置缺省时，使用依赖方数据源，依赖方无数据源则启动报错
        useDefaultRdb: false
        rdb:
          driver: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://rdb.jmqtt.com:3306/jmqtt?characterEncoding=utf8&autoReconnect=true&failOverReadOnly=false&useSSL=false&serverTimezone=Asia/Shanghai
          username: root
          password: jmqtt@2020
        # redis数据源，逻辑同rdb
        useDefaultRedis: false
        redis:
          redis-host: rdb.jmqtt.com
          redis-port: 6379
          redis-password: jmqtt@2020
          database: 11
          max-wait-mills: 60000
          max-idle: 50
          min-idle: 20
          max-total: 200
        netty:
          # 服务监听的tcp及websocket端口
          tcp-port: 1883
          ssl-tcp-port: 1884
          websocket-port: 8883
          ssl-websocket-port: 8884
          # ssl证书
          ssl-key-store-type: PKCS12
          ssl-key-file-path: conf/server.pfx
          ssl-manager-pwd: 654321
          ssl-store-pwd: 654321
          # 是否使用epoll，linux环境下有效
          use-epoll: true
          # 是否开启池化的buf
          pooled-byte-buf-allocator-enable: true
        akka:
          # 是否开启Akka，开启后提高吞吐，集群环境下建议开启
          # Akka作用是将一个broker节点的消息同步到其他broker节点
          # 集群环境下，store为mem时，Akka必须开启
          enable: false
          # 当前Akka集群系统名称
          system-name: JMqttDispatcherSystem
          # 当前节点在集群中的ip和端口
          host: 127.0.0.1
          port: 25251
          # akka集群中所有节点，可以只配置集群中任意非当前节点。
          cluster-nodes:
            - 127.0.0.1:25251
            - 127.0.0.1:25252
# 以上配置均为可选，意味着引入依赖后直接启动项目即可，真正做到开箱即用。
```

3.集群环境下
+ 如果选择关系型数据库（store=rdb）作为存储，那么所有节点都必须连接到同一个数据库；
+ 如果选择redis作为存储（store=redis），所有节点也必须连接到同一个redis；
+ 选择内存存储时（store=mem），要开启akka，此时性能是最高的；
+ 选择rdb或者redis时同样可以开启akka；
+ 单节点环境下开启akka无意义。

4.网关

```
可以使用nginx作为网关，利用upstream实现对外只暴露一个ip和端口，请求路由到后置服务不同jmqtt broker节点。

stream {
 
 upstream jmqtt_tcp_nodes {
    # 默认负载策略是轮询，这样同一个客户端每次连接可能负载到后置不同的节点，造成session被接管，此时需要清理上一个
    # 连接节点的session，当前节点再重新创建session，造成资源浪费。个人建议使用hash，这样能保证客户端每次都能连接
    # 到同一个服务，频繁重连情况下可以避免后端集群出现session被接管的情况。
    # least_conn;最小连接数，请求分发到连接最少的节点
    # 根据实际情况选择合适的负载策略。
    ip_hash;
    server dev1.jmqtt.com:1884;
    server dev2.jmqtt.com:1884;
    server dev3.jmqtt.com:1884;
  }

  server {
    listen 8883 ssl;
    ssl_session_timeout 30m;
    ssl_certificate /home/root/nginx/conf/server.pem;
    ssl_certificate_key /home/root/nginx/conf/server.key;

    proxy_pass jmqtt_tcp_nodes;

  }
}

同理websocket代理配置如下：

http {
  upstream jmqtt_websocket_nodes {
    ip_hash;
    server dev1.jmqtt.com:8884;
    server dev2.jmqtt.com:8884;
    server dev3.jmqtt.com:8884;
  }

  server {
    listen 443 ssl;
    ssl_session_timeout 30m;
    ssl_certificate /home/root/nginx/conf/server.pem;
    ssl_certificate_key /home/root/nginx/conf/server.key;

    location /mqtt {
        proxy_pass http://jmqtt_websocket_nodes;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
    }
  }
}

```

5.Mqtt5

    支持mqtt5所有功能。

6.Future
+ 集群环境下订阅树
+ 集群共享订阅，目前只支持本地共享订阅
+ 选择合适的序列化工具（比如Kryo），目前简单粗暴转为字符串
+ JMQTT Keeper
+ $sys、$file
+ mqtt5 reasonCode完善
+ 升级JDK21，将线程池替换为虚拟线程（类似go中的协程）

