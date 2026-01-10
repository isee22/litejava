# 游戏长连接管理

## 核心挑战

| 挑战 | 说明 | 影响 |
|------|------|------|
| 连接数量 | 单服 10万+ 连接 | 内存、文件描述符 |
| 连接分配 | 玩家连到哪台网关 | 负载均衡 |
| 连接迁移 | 切换场景/房间 | 状态同步 |
| 断线重连 | 网络抖动恢复 | 用户体验 |
| 心跳检测 | 识别死连接 | 资源回收 |

## 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              客户端                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           负载均衡 (LB)                                      │
│  - DNS 轮询                                                                  │
│  - Nginx/HAProxy                                                            │
│  - 云厂商 SLB                                                                │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
              ▼                       ▼                       ▼
┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
│     网关服务器 1       │ │     网关服务器 2       │ │     网关服务器 N       │
│  ┌─────────────────┐  │ │  ┌─────────────────┐  │ │  ┌─────────────────┐  │
│  │ 连接管理器       │  │ │  │ 连接管理器       │  │ │  │ 连接管理器       │  │
│  │ - 10000 连接    │  │ │  │ - 10000 连接    │  │ │  │ - 10000 连接    │  │
│  └─────────────────┘  │ │  └─────────────────┘  │ │  └─────────────────┘  │
│  ┌─────────────────┐  │ │  ┌─────────────────┐  │ │  ┌─────────────────┐  │
│  │ 会话管理器       │  │ │  │ 会话管理器       │  │ │  │ 会话管理器       │  │
│  │ - 玩家ID映射    │  │ │  │ - 玩家ID映射    │  │ │  │ - 玩家ID映射    │  │
│  └─────────────────┘  │ │  └─────────────────┘  │ │  └─────────────────┘  │
└───────────────────────┘ └───────────────────────┘ └───────────────────────┘
              │                       │                       │
              └───────────────────────┼───────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           连接注册中心 (Redis)                               │
│  - 玩家ID → 网关ID 映射                                                      │
│  - 网关负载信息                                                              │
│  - 在线玩家列表                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           业务服务器集群                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  房间服务器  │  │  场景服务器  │  │  匹配服务器  │  │  聊天服务器  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 连接管理器

### 核心数据结构

```java
public class ConnectionManager {
    // 连接ID → 连接对象
    private final Map<Long, Connection> connections = new ConcurrentHashMap<>();
    
    // 玩家ID → 连接ID
    private final Map<Long, Long> playerConnections = new ConcurrentHashMap<>();
    
    // 连接ID生成器
    private final AtomicLong connIdGenerator = new AtomicLong(0);
    
    // 网关ID (全局唯一)
    private final int gatewayId;
    
    // 最大连接数
    private final int maxConnections = 10000;
}
```

### 连接生命周期

```
┌─────────┐    握手成功    ┌─────────┐    登录成功    ┌─────────┐
│ PENDING │──────────────▶│CONNECTED│──────────────▶│  AUTHED │
└─────────┘               └─────────┘               └─────────┘
     │                         │                         │
     │ 超时/失败               │ 心跳超时                 │ 主动断开
     ▼                         ▼                         ▼
┌─────────┐               ┌─────────┐               ┌─────────┐
│ CLOSED  │               │ CLOSED  │               │ CLOSED  │
└─────────┘               └─────────┘               └─────────┘
```

### 连接管理实现

```java
public class Connection {
    public long connId;              // 连接ID
    public long playerId;            // 玩家ID (登录后)
    public Channel channel;          // Netty Channel
    public int state;                // 连接状态
    public long createTime;          // 创建时间
    public long lastActiveTime;      // 最后活跃时间
    public String clientIp;          // 客户端IP
    public int gatewayId;            // 所属网关
}

public class ConnectionManager {
    
    // 新连接
    public Connection onConnect(Channel channel) {
        if (connections.size() >= maxConnections) {
            channel.close();
            return null;
        }
        
        Connection conn = new Connection();
        conn.connId = connIdGenerator.incrementAndGet();
        conn.channel = channel;
        conn.state = State.CONNECTED;
        conn.createTime = System.currentTimeMillis();
        conn.lastActiveTime = conn.createTime;
        conn.gatewayId = this.gatewayId;
        
        connections.put(conn.connId, conn);
        return conn;
    }
    
    // 登录绑定
    public void bindPlayer(long connId, long playerId) {
        Connection conn = connections.get(connId);
        if (conn == null) return;
        
        // 踢掉旧连接
        Long oldConnId = playerConnections.get(playerId);
        if (oldConnId != null && !oldConnId.equals(connId)) {
            kickConnection(oldConnId, "duplicate_login");
        }
        
        conn.playerId = playerId;
        conn.state = State.AUTHED;
        playerConnections.put(playerId, connId);
        
        // 注册到 Redis
        redis.hset("player:gateway", String.valueOf(playerId), String.valueOf(gatewayId));
        redis.sadd("gateway:" + gatewayId + ":players", String.valueOf(playerId));
    }
    
    // 断开连接
    public void onDisconnect(long connId) {
        Connection conn = connections.remove(connId);
        if (conn == null) return;
        
        if (conn.playerId > 0) {
            playerConnections.remove(conn.playerId);
            
            // 从 Redis 移除
            redis.hdel("player:gateway", String.valueOf(conn.playerId));
            redis.srem("gateway:" + gatewayId + ":players", String.valueOf(conn.playerId));
            
            // 通知业务服务器
            notifyPlayerOffline(conn.playerId);
        }
    }
    
    // 发送消息给玩家
    public void sendToPlayer(long playerId, Object msg) {
        Long connId = playerConnections.get(playerId);
        if (connId == null) return;
        
        Connection conn = connections.get(connId);
        if (conn != null && conn.channel.isActive()) {
            conn.channel.writeAndFlush(msg);
        }
    }
    
    // 广播消息
    public void broadcast(Collection<Long> playerIds, Object msg) {
        for (Long playerId : playerIds) {
            sendToPlayer(playerId, msg);
        }
    }
}
```

## 心跳检测

```java
public class HeartbeatManager {
    private final int heartbeatInterval = 30000;  // 30秒
    private final int heartbeatTimeout = 90000;   // 90秒超时
    
    // 定时检测
    public void startHeartbeatCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            
            for (Connection conn : connectionManager.getAllConnections()) {
                if (now - conn.lastActiveTime > heartbeatTimeout) {
                    // 心跳超时，断开连接
                    connectionManager.kickConnection(conn.connId, "heartbeat_timeout");
                }
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }
    
    // 收到心跳
    public void onHeartbeat(long connId) {
        Connection conn = connectionManager.getConnection(connId);
        if (conn != null) {
            conn.lastActiveTime = System.currentTimeMillis();
        }
    }
}
```

## 负载均衡与网关选择

### 网关负载上报

```java
public class GatewayLoadReporter {
    
    // 每 5 秒上报一次负载
    public void startReport() {
        scheduler.scheduleAtFixedRate(() -> {
            GatewayLoad load = new GatewayLoad();
            load.gatewayId = gatewayId;
            load.connectionCount = connectionManager.getConnectionCount();
            load.maxConnections = maxConnections;
            load.cpuUsage = getCpuUsage();
            load.memoryUsage = getMemoryUsage();
            load.timestamp = System.currentTimeMillis();
            
            // 上报到 Redis
            redis.hset("gateway:load", String.valueOf(gatewayId), JSON.toJSONString(load));
            redis.expire("gateway:load", 30);
        }, 0, 5, TimeUnit.SECONDS);
    }
}
```

### 网关选择策略

```java
public class GatewaySelector {
    
    // 选择最优网关
    public GatewayInfo selectGateway() {
        Map<String, String> loads = redis.hgetAll("gateway:load");
        
        GatewayInfo best = null;
        double minScore = Double.MAX_VALUE;
        
        for (Map.Entry<String, String> entry : loads.entrySet()) {
            GatewayLoad load = JSON.parseObject(entry.getValue(), GatewayLoad.class);
            
            // 跳过满载网关
            if (load.connectionCount >= load.maxConnections * 0.9) {
                continue;
            }
            
            // 计算负载分数 (越低越好)
            double score = calculateScore(load);
            if (score < minScore) {
                minScore = score;
                best = getGatewayInfo(load.gatewayId);
            }
        }
        
        return best;
    }
    
    private double calculateScore(GatewayLoad load) {
        // 连接数权重 0.5，CPU 权重 0.3，内存权重 0.2
        double connRatio = (double) load.connectionCount / load.maxConnections;
        return connRatio * 0.5 + load.cpuUsage * 0.3 + load.memoryUsage * 0.2;
    }
}
```

## 跨网关通信

### 场景：玩家 A 在网关1，要给玩家 B (网关2) 发消息

```
┌─────────────┐                              ┌─────────────┐
│   网关 1    │                              │   网关 2    │
│  (玩家 A)   │                              │  (玩家 B)   │
└──────┬──────┘                              └──────┬──────┘
       │                                            │
       │  1. 查询 B 在哪个网关                        │
       │ ─────────────────────▶ Redis               │
       │                                            │
       │  2. 发送跨网关消息                          │
       │ ─────────────────────────────────────────▶ │
       │         (通过 MQ 或直连)                    │
       │                                            │
       │                              3. 推送给 B    │
       │                              ◀──────────── │
```

### 实现方式

```java
public class CrossGatewayMessenger {
    
    // 发送消息给任意玩家
    public void sendToPlayer(long targetPlayerId, Object msg) {
        // 1. 查询目标玩家在哪个网关
        String gatewayIdStr = redis.hget("player:gateway", String.valueOf(targetPlayerId));
        
        if (gatewayIdStr == null) {
            // 玩家不在线
            return;
        }
        
        int targetGatewayId = Integer.parseInt(gatewayIdStr);
        
        if (targetGatewayId == this.gatewayId) {
            // 在本网关，直接发送
            connectionManager.sendToPlayer(targetPlayerId, msg);
        } else {
            // 跨网关，通过 MQ 转发
            CrossGatewayMessage crossMsg = new CrossGatewayMessage();
            crossMsg.targetGatewayId = targetGatewayId;
            crossMsg.targetPlayerId = targetPlayerId;
            crossMsg.message = msg;
            
            mqProducer.send("gateway.message." + targetGatewayId, crossMsg);
        }
    }
}

// 网关监听跨网关消息
public class CrossGatewayListener {
    
    @MQListener(topic = "gateway.message.${gatewayId}")
    public void onCrossMessage(CrossGatewayMessage msg) {
        connectionManager.sendToPlayer(msg.targetPlayerId, msg.message);
    }
}
```

## 断线重连

### 重连流程

```
┌─────────┐                    ┌─────────┐                    ┌─────────┐
│  客户端  │                    │  网关    │                    │ 业务服务 │
└────┬────┘                    └────┬────┘                    └────┬────┘
     │                              │                              │
     │  1. 断线                     │                              │
     │ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ▶│                              │
     │                              │                              │
     │                              │  2. 标记离线 (保留状态 5 分钟) │
     │                              │─────────────────────────────▶│
     │                              │                              │
     │  3. 重连请求 (带 token)      │                              │
     │ ────────────────────────────▶│                              │
     │                              │                              │
     │                              │  4. 验证 token               │
     │                              │─────────────────────────────▶│
     │                              │                              │
     │                              │  5. 恢复会话                  │
     │                              │◀─────────────────────────────│
     │                              │                              │
     │  6. 同步状态                 │                              │
     │ ◀────────────────────────────│                              │
     │                              │                              │
```

### 重连实现

```java
public class ReconnectManager {
    // 断线会话缓存 (5分钟过期)
    private final Cache<Long, ReconnectSession> sessions = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    // 玩家断线
    public void onDisconnect(long playerId, long connId) {
        ReconnectSession session = new ReconnectSession();
        session.playerId = playerId;
        session.oldConnId = connId;
        session.token = generateToken();
        session.disconnectTime = System.currentTimeMillis();
        
        sessions.put(playerId, session);
        
        // 通知业务服务器：玩家断线但可重连
        notifyPlayerDisconnect(playerId, true);
    }
    
    // 玩家重连
    public boolean onReconnect(long playerId, String token, long newConnId) {
        ReconnectSession session = sessions.getIfPresent(playerId);
        
        if (session == null) {
            // 会话已过期
            return false;
        }
        
        if (!session.token.equals(token)) {
            // token 不匹配
            return false;
        }
        
        // 重连成功
        sessions.invalidate(playerId);
        
        // 绑定新连接
        connectionManager.bindPlayer(newConnId, playerId);
        
        // 通知业务服务器：玩家重连成功
        notifyPlayerReconnect(playerId);
        
        return true;
    }
}
```

## 分区架构

### 分区模型

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              全局服务                                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                         │
│  │  登录服务器  │  │  账号服务器  │  │  支付服务器  │                         │
│  └─────────────┘  └─────────────┘  └─────────────┘                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
              ▼                       ▼                       ▼
┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
│        1 区           │ │        2 区           │ │        N 区           │
│  ┌─────────────────┐  │ │  ┌─────────────────┐  │ │  ┌─────────────────┐  │
│  │     网关集群     │  │ │  │     网关集群     │  │ │  │     网关集群     │  │
│  └─────────────────┘  │ │  └─────────────────┘  │ │  └─────────────────┘  │
│  ┌─────────────────┐  │ │  ┌─────────────────┐  │ │  ┌─────────────────┐  │
│  │    游戏服务器    │  │ │  │    游戏服务器    │  │ │  │    游戏服务器    │  │
│  └─────────────────┘  │ │  └─────────────────┘  │ │  └─────────────────┘  │
│  ┌─────────────────┐  │ │  ┌─────────────────┐  │ │  ┌─────────────────┐  │
│  │    数据库集群    │  │ │  │    数据库集群    │  │ │  │    数据库集群    │  │
│  └─────────────────┘  │ │  └─────────────────┘  │ │  └─────────────────┘  │
└───────────────────────┘ └───────────────────────┘ └───────────────────────┘
              │                       │                       │
              └───────────────────────┼───────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              跨服服务                                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                         │
│  │  跨服战场   │  │  跨服拍卖   │  │  跨服聊天   │                         │
│  └─────────────┘  └─────────────┘  └─────────────┘                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 分区配置

```java
public class ZoneConfig {
    public int zoneId;              // 区ID
    public String zoneName;         // 区名称
    public List<String> gateways;   // 网关地址列表
    public int maxPlayers;          // 最大玩家数
    public int status;              // 0:维护 1:流畅 2:拥挤 3:爆满
    public boolean isNew;           // 是否新区
    public boolean isRecommend;     // 是否推荐
}

// 获取区列表
public List<ZoneConfig> getZoneList() {
    return redis.hgetAll("zones").values().stream()
        .map(json -> JSON.parseObject(json, ZoneConfig.class))
        .sorted(Comparator.comparing(z -> z.zoneId))
        .collect(Collectors.toList());
}
```

## 性能优化

| 优化点 | 方案 | 效果 |
|--------|------|------|
| 连接数 | Netty + Epoll | 单机 10万+ |
| 内存 | 对象池复用 | 减少 GC |
| 序列化 | Protobuf | 减少带宽 |
| 心跳 | 时间轮 | 减少定时器 |
| 广播 | 批量发送 | 减少系统调用 |
