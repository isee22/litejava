# Hall Server - 大厅服务器

BabyKylin 模式的核心调度服务，负责房间管理和 GameServer 负载均衡。

## 职责

```
┌─────────────────────────────────────────────────────────────┐
│                      Hall Server                             │
├─────────────────────────────────────────────────────────────┤
│  面向 GameServer:                                            │
│  - /register_gs    GameServer 注册                          │
│  - /heartbeat      心跳 (上报负载)                           │
│  - /room_destroyed 房间销毁通知                              │
├─────────────────────────────────────────────────────────────┤
│  面向客户端:                                                 │
│  - /create_private_room  创建私人房间                        │
│  - /enter_private_room   加入私人房间                        │
│  - /match/start          开始匹配                            │
│  - /match/cancel         取消匹配                            │
│  - /get_user_room        获取用户房间状态                    │
├─────────────────────────────────────────────────────────────┤
│  内部功能:                                                   │
│  - 6位数字房间号生成 (100000-999999)                         │
│  - GameServer 负载均衡                                       │
│  - 匹配队列管理                                              │
│  - 房间位置缓存                                              │
└─────────────────────────────────────────────────────────────┘
```

## 架构

```
                    ┌─────────────────┐
                    │   Hall Server   │
                    │     (8201)      │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  serverMap    │    │  matchQueues  │    │    cache      │
│               │    │               │    │               │
│ id → Server   │    │ queue → users │    │ room → server │
│ 负载均衡选择   │    │ 定时匹配处理  │    │ user → room   │
└───────────────┘    └───────────────┘    └───────────────┘
```

## 配置

`src/main/resources/application.yml`:
```yaml
server:
  httpPort: 8201
  priKey: ROOM_PRI_KEY_2024   # 签名密钥

# 可选 Redis (用于集群部署)
# redis:
#   host: localhost
#   port: 6379
```

## API

### GameServer 接口

#### 注册 GameServer
```
GET /register_gs?clientip={ip}&clientport={wsPort}&httpPort={httpPort}&load={load}&gameType={type}

响应: {"code": 0, "data": {"ip": "实际IP"}}
```

#### 心跳
```
GET /heartbeat?id={serverId}&load={load}

响应: {"code": 0}
```

### 客户端接口

#### 创建私人房间
```
GET /create_private_room?userId={userId}&gameType={type}&conf={}

响应: {
  "code": 0,
  "data": {
    "roomid": "123456",    // 6位房间号
    "ip": "192.168.1.100",
    "port": 9100,
    "token": "xxx",
    "time": 1234567890,
    "sign": "md5签名"
  }
}
```

#### 加入私人房间
```
GET /enter_private_room?userId={userId}&roomId={roomId}&name={name}

响应: 同上
```

#### 开始匹配
```
POST /match/start
{
  "userId": 1001,
  "gameType": "doudizhu",
  "level": "normal",
  "name": "玩家名"
}

响应 (匹配成功):
{
  "code": 0,
  "data": {
    "status": "matched",
    "roomid": "234567",
    "ip": "...",
    "port": 9100,
    "token": "xxx"
  }
}

响应 (等待中):
{"code": 0, "data": {"status": "matching"}}
```

#### 取消匹配
```
POST /match/cancel
{"userId": 1001}
```

### 管理接口

```
GET /admin/servers       # 所有 GameServer 状态
GET /admin/match_queues  # 匹配队列状态
POST /admin/clear_room   # 强制清理房间
```

## 负载均衡

### 选择策略

```java
ServerInfo chooseServer(String gameType) {
    1. 过滤超时服务器 (60秒无心跳)
    2. 过滤 gameType 匹配的服务器
    3. 选择 load 最低的服务器
}
```

### 负载计算

GameServer 上报: `load = rooms * 10 + players`

示例:
- 服务器 A: 5 个房间, 15 个玩家 → load = 65
- 服务器 B: 3 个房间, 9 个玩家 → load = 39
- 选择服务器 B

## 房间号生成

```java
// 6位数字房间号 (100000-999999)
private String generateRoomId() {
    for (int i = 0; i < 100; i++) {
        long counter = roomIdCounter.incrementAndGet();
        long roomNum = 100000 + (counter % 900000);
        String roomId = String.valueOf(roomNum);
        
        // 检查是否已被占用
        if (cache.get(KEY_ROOM_SERVER + roomId) == null) {
            return roomId;
        }
    }
    // 降级到 UUID
    return UUID.randomUUID().toString().substring(0, 8);
}
```

## 匹配队列

### 队列配置

```java
queueConfigs.put("doudizhu:normal", 3);  // 斗地主 3 人
queueConfigs.put("mahjong:normal", 4);   // 麻将 4 人
queueConfigs.put("gobang:normal", 2);    // 五子棋 2 人
```

### 匹配流程

```
1. 玩家调用 /match/start
2. 加入对应队列 (gameType:level)
3. 同步等待最多 5 秒
4. 后台每秒检查队列:
   - 人数足够 → 创建房间 → 通知所有玩家
   - 人数不够 → 继续等待
5. 超时返回 {status: "matching"}
6. 客户端可再次调用继续等待
```

## 签名验证

Hall ↔ GameServer 通信使用 MD5 签名:

```java
// 创建房间
sign = md5(userId + roomId + conf + ROOM_PRI_KEY)

// 进入房间
sign = md5(userId + name + roomId + ROOM_PRI_KEY)

// 检查房间
sign = md5(roomId + ROOM_PRI_KEY)
```

## 启动

```bash
mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer
```

## 独立性

Hall Server 不依赖 room-game-common，只依赖 litejava-plugins:

```xml
<dependencies>
    <dependency>
        <groupId>litejava</groupId>
        <artifactId>litejava-plugins</artifactId>
    </dependency>
</dependencies>
```

这意味着:
- 可以独立部署和扩展
- 不关心具体游戏逻辑
- 只负责路由和调度
