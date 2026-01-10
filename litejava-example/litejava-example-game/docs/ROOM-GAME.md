# 房间制游戏架构详解

## 适用场景

- 棋牌游戏 (斗地主、麻将、德州扑克)
- MOBA 游戏 (DOTA2、王者荣耀)
- FPS 游戏 (CS、PUBG)
- 战旗游戏 (火焰纹章、云顶之弈)
- 卡牌对战 (炉石传说、游戏王)

## 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              客户端                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         网关服务器集群 (Gateway)                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                         │
│  │  Gateway-1  │  │  Gateway-2  │  │  Gateway-N  │  ← 可水平扩展            │
│  │  10000连接  │  │  10000连接  │  │  10000连接  │                         │
│  └─────────────┘  └─────────────┘  └─────────────┘                         │
│  职责：长连接管理、协议解析、心跳检测、消息路由                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────────┐     ┌───────────────────┐     ┌───────────────────────┐
│   大厅服务器集群   │     │   匹配服务器集群   │     │     房间服务器集群      │
│    (Lobby)        │     │    (Match)        │     │      (Room)           │
│  ┌─────────────┐  │     │  ┌─────────────┐  │     │  ┌─────────────────┐  │
│  │  Lobby-1   │  │     │  │  Match-1   │  │     │  │   Room-1        │  │
│  │  Lobby-2   │  │     │  │  Match-2   │  │     │  │   1000房间      │  │
│  └─────────────┘  │     │  └─────────────┘  │     │  ├─────────────────┤  │
│                   │     │                   │     │  │   Room-2        │  │
│  职责：           │     │  职责：           │     │  │   1000房间      │  │
│  - 玩家登录/登出  │     │  - 匹配队列管理   │     │  └─────────────────┘  │
│  - 玩家信息查询  │     │  - ELO/段位匹配   │     │                       │
│  - 好友系统      │     │  - 房间分配       │     │  职责：               │
│  - 邮件系统      │     │  - 组队匹配       │     │  - 房间生命周期管理   │
│  - 商城/背包     │     │                   │     │  - 游戏逻辑执行       │
└───────────────────┘     └───────────────────┘     │  - 状态同步/广播     │
        │                             │             └───────────────────────┘
        │                             │                         │
        └─────────────────────────────┼─────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           聊天服务器集群 (Chat)                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                         │
│  │   Chat-1    │  │   Chat-2    │  │   Chat-N    │  ← 按频道分片            │
│  └─────────────┘  └─────────────┘  └─────────────┘                         │
│  职责：世界聊天、私聊、房间聊天、公会聊天                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           全局服务 (无状态)                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   排行榜    │  │   公告      │  │   活动      │  │   支付      │        │
│  │  Ranking   │  │  Notice    │  │  Activity  │  │  Payment   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│  特点：无状态、可任意扩展、通过 HTTP/RPC 调用                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              基础设施层                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Redis集群   │  │ MySQL主从   │  │ RabbitMQ   │  │  Consul     │        │
│  │ (缓存/会话) │  │ (持久化)    │  │ (消息队列)  │  │ (服务发现)  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 服务器职责详解

### 1. 网关服务器 (Gateway)

核心职责：管理客户端长连接，是所有请求的入口

```java
public class GatewayServer {
    // 连接管理
    private ConnectionManager connManager;
    
    // 消息路由表：消息ID → 目标服务
    private Map<Integer, String> routeTable = Map.of(
        1001, "lobby",    // 登录
        1002, "lobby",    // 玩家信息
        2001, "match",    // 开始匹配
        2002, "match",    // 取消匹配
        3001, "room",     // 房间操作
        3002, "room",     // 游戏操作
        4001, "chat"      // 聊天消息
    );
    
    // 消息路由
    public void onMessage(Connection conn, Message msg) {
        String target = routeTable.get(msg.msgId);
        forwardToService(target, conn.playerId, msg);
    }
}
```

特点：
- 有状态 (维护连接)
- 按连接数扩展
- 单机 1-2 万连接

### 2. 大厅服务器 (Lobby)

核心职责：处理非游戏内的玩家操作

```java
public class LobbyServer {
    
    // 玩家登录
    public void onLogin(long playerId, String token) {
        // 1. 验证 token
        // 2. 加载玩家数据
        // 3. 注册到在线列表
        // 4. 返回玩家信息
    }
    
    // 获取玩家信息
    public PlayerInfo getPlayerInfo(long playerId) {
        return playerCache.get(playerId);
    }
    
    // 好友列表
    public List<Friend> getFriends(long playerId) {
        return friendService.getFriends(playerId);
    }
    
    // 商城购买
    public void buyItem(long playerId, int itemId) {
        shopService.buy(playerId, itemId);
    }
}
```

特点：
- 无状态 (数据在 Redis/MySQL)
- 可任意水平扩展
- 请求量大但逻辑简单

### 3. 匹配服务器 (Match)

核心职责：玩家匹配、房间分配

```java
public class MatchServer {
    // 匹配队列 (按模式分)
    private Map<Integer, MatchQueue> queues = new ConcurrentHashMap<>();
    
    // 开始匹配
    public void startMatch(long playerId, int mode) {
        MatchQueue queue = queues.get(mode);
        queue.add(new MatchPlayer(playerId, getElo(playerId)));
    }
    
    // 匹配循环 (定时执行)
    public void matchLoop() {
        for (MatchQueue queue : queues.values()) {
            List<MatchGroup> groups = queue.tryMatch();
            for (MatchGroup group : groups) {
                // 分配房间服务器
                RoomServer server = selectRoomServer();
                // 创建房间
                long roomId = server.createRoom(group.getPlayerIds());
                // 通知玩家进入房间
                notifyEnterRoom(group.getPlayerIds(), roomId, server.getAddress());
            }
        }
    }
    
    // ELO 匹配算法
    private List<MatchGroup> tryMatch(MatchQueue queue) {
        // 按 ELO 分段
        // 同段位优先匹配
        // 等待时间越长，匹配范围越宽
    }
}
```

特点：
- 有状态 (匹配队列)
- 按游戏模式分片
- 需要高效匹配算法

### 4. 房间服务器 (Room)

核心职责：游戏逻辑执行、状态同步

```java
public class RoomServer {
    // 房间管理
    private Map<Long, Room> rooms = new ConcurrentHashMap<>();
    
    // 创建房间
    public long createRoom(List<Long> playerIds, int roomType) {
        Room room = new Room();
        room.roomId = idGenerator.next();
        room.roomType = roomType;
        room.players = playerIds.stream()
            .map(this::loadPlayer)
            .collect(Collectors.toList());
        room.status = RoomStatus.WAITING;
        
        rooms.put(room.roomId, room);
        return room.roomId;
    }
    
    // 游戏操作
    public void onGameAction(long roomId, long playerId, GameAction action) {
        Room room = rooms.get(roomId);
        if (room == null) return;
        
        // 执行游戏逻辑
        GameResult result = gameLogic.execute(room, playerId, action);
        
        // 广播给房间内所有玩家
        broadcastToRoom(room, result);
        
        // 检查游戏结束
        if (room.isGameOver()) {
            settleRoom(room);
        }
    }
    
    // 房间广播
    private void broadcastToRoom(Room room, Object msg) {
        for (Player player : room.players) {
            gateway.sendToPlayer(player.id, msg);
        }
    }
}
```

特点：
- 有状态 (房间数据)
- 按房间数扩展
- 单机 1000-2000 房间

### 5. 聊天服务器 (Chat)

核心职责：各类聊天消息处理

```java
public class ChatServer {
    // 频道订阅 (玩家ID → 订阅的频道)
    private Map<Long, Set<String>> subscriptions = new ConcurrentHashMap<>();
    
    // 世界聊天
    public void worldChat(long playerId, String content) {
        ChatMessage msg = new ChatMessage(playerId, "world", content);
        // 广播给所有在线玩家 (通过 MQ 分发到各网关)
        mq.publish("chat.world", msg);
    }
    
    // 私聊
    public void privateChat(long fromId, long toId, String content) {
        ChatMessage msg = new ChatMessage(fromId, "private", content);
        gateway.sendToPlayer(toId, msg);
    }
    
    // 房间聊天
    public void roomChat(long playerId, long roomId, String content) {
        ChatMessage msg = new ChatMessage(playerId, "room", content);
        // 获取房间玩家列表
        List<Long> players = roomService.getRoomPlayers(roomId);
        gateway.sendToPlayers(players, msg);
    }
}
```

特点：
- 按频道分片
- 世界聊天需要 MQ 广播
- 私聊/房间聊天点对点

### 6. 全局服务 (无状态)

```java
// 排行榜服务
public class RankingService {
    public List<RankItem> getTopN(String rankType, int n) {
        return redis.zrevrange("rank:" + rankType, 0, n - 1);
    }
    
    public void updateScore(String rankType, long playerId, long score) {
        redis.zadd("rank:" + rankType, score, String.valueOf(playerId));
    }
}

// 公告服务
public class NoticeService {
    public List<Notice> getNotices() {
        return noticeDao.findActive();
    }
}

// 活动服务
public class ActivityService {
    public List<Activity> getActivities(long playerId) {
        return activityDao.findByPlayer(playerId);
    }
}
```

特点：
- 完全无状态
- 通过 HTTP/RPC 调用
- 可任意扩展

## 消息流转示例

### 玩家登录流程

```
┌────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ 客户端  │     │  网关    │     │  大厅    │     │  Redis  │
└───┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
    │               │               │               │
    │  1.建立连接   │               │               │
    │──────────────▶│               │               │
    │               │               │               │
    │  2.登录请求   │               │               │
    │──────────────▶│  3.转发       │               │
    │               │──────────────▶│               │
    │               │               │  4.验证token  │
    │               │               │──────────────▶│
    │               │               │◀──────────────│
    │               │               │               │
    │               │               │  5.加载数据   │
    │               │               │──────────────▶│
    │               │               │◀──────────────│
    │               │               │               │
    │               │  6.登录成功   │               │
    │               │◀──────────────│               │
    │  7.返回结果   │               │               │
    │◀──────────────│               │               │
```

### 匹配并进入房间流程

```
┌────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│ 客户端  │   │  网关    │   │  匹配    │   │  房间    │   │  Redis  │
└───┬────┘   └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘
    │             │             │             │             │
    │ 1.开始匹配  │             │             │             │
    │────────────▶│ 2.转发      │             │             │
    │             │────────────▶│             │             │
    │             │             │ 3.加入队列  │             │
    │             │             │────────────────────────▶ │
    │             │             │             │             │
    │             │             │ 4.匹配成功  │             │
    │             │             │ (定时检测)  │             │
    │             │             │             │             │
    │             │             │ 5.创建房间  │             │
    │             │             │────────────▶│             │
    │             │             │             │ 6.保存房间  │
    │             │             │             │────────────▶│
    │             │             │◀────────────│             │
    │             │             │             │             │
    │             │ 7.进入房间  │             │             │
    │             │◀────────────│             │             │
    │ 8.通知客户端│             │             │             │
    │◀────────────│             │             │             │
    │             │             │             │             │
    │ 9.加载房间  │             │             │             │
    │────────────▶│────────────────────────▶ │             │
    │             │             │             │ 10.同步状态 │
    │◀────────────│◀────────────────────────│             │
```

## 核心概念

### 房间 (Room)

```java
public class Room {
    public long roomId;
    public int roomType;           // 房间类型
    public int status;             // 0:等待 1:游戏中 2:结算
    public List<Player> players;   // 玩家列表
    public GameState state;        // 游戏状态
    public long createTime;
    public long startTime;
}
```

### 房间生命周期

```
创建房间 → 等待玩家 → 游戏开始 → 游戏进行 → 游戏结束 → 结算 → 销毁房间
   │          │          │          │          │        │
   ▼          ▼          ▼          ▼          ▼        ▼
 CREATED   WAITING   STARTING   PLAYING    ENDING   FINISHED
```

## 同步模式

### 帧同步 (Lockstep)

适用于：MOBA、RTS、格斗

```
客户端 A ──┐                    ┌── 客户端 A
           │    ┌─────────┐    │
客户端 B ──┼───▶│ 服务器  │───▶┼── 客户端 B
           │    │ (转发)  │    │
客户端 C ──┘    └─────────┘    └── 客户端 C

特点：
- 服务器只转发输入，不计算逻辑
- 客户端本地计算，结果一致
- 带宽小，但对延迟敏感
- 需要确定性算法 (定点数)
```

### 状态同步 (State Sync)

适用于：棋牌、回合制、FPS

```
客户端 A ──┐                    ┌── 客户端 A
           │    ┌─────────┐    │
客户端 B ──┼───▶│ 服务器  │───▶┼── 客户端 B
           │    │ (计算)  │    │
客户端 C ──┘    └─────────┘    └── 客户端 C

特点：
- 服务器计算逻辑，广播状态
- 客户端只负责渲染
- 防作弊，服务器权威
- 带宽大，但延迟容忍度高
```

## 棋牌游戏示例 (斗地主)

### 房间状态机

```
┌─────────┐    玩家加入    ┌─────────┐    人满    ┌─────────┐
│ WAITING │──────────────▶│ READY   │──────────▶│ BIDDING │
└─────────┘               └─────────┘           └─────────┘
                                                     │
                                                叫地主完成
                                                     │
┌─────────┐    出完牌     ┌─────────┐            ┌───▼─────┐
│ SETTLE  │◀─────────────│ PLAYING │◀───────────│ DEALING │
└─────────┘               └─────────┘            └─────────┘
```

### 核心代码结构

```java
// 房间
public class DoudizhuRoom extends Room {
    public int landlordSeat;       // 地主座位
    public int currentSeat;        // 当前出牌座位
    public List<Card> lastCards;   // 上一手牌
    public int passCount;          // 连续 pass 次数
}

// 玩家
public class DoudizhuPlayer extends Player {
    public int seat;               // 座位号 0,1,2
    public List<Card> handCards;   // 手牌
    public boolean isLandlord;     // 是否地主
}

// 游戏逻辑
public class DoudizhuLogic {
    public void deal(Room room);           // 发牌
    public void bid(Room room, int seat);  // 叫地主
    public boolean playCards(Room room, int seat, List<Card> cards);  // 出牌
    public void settle(Room room);         // 结算
}
```

## 战旗游戏示例 (回合制)

### 房间状态机

```
┌─────────┐    匹配成功    ┌─────────┐    加载完成   ┌─────────┐
│ MATCHING│──────────────▶│ LOADING │─────────────▶│ PLAYING │
└─────────┘               └─────────┘              └─────────┘
                                                        │
                                                   回合循环
                                                        │
┌─────────┐    一方全灭    ┌─────────┐            ┌─────▼───┐
│ SETTLE  │◀─────────────│ ENDING  │◀───────────│  TURN   │
└─────────┘               └─────────┘            └─────────┘
```

### 回合制逻辑

```java
public class TurnBasedRoom extends Room {
    public int currentTurn;        // 当前回合数
    public int currentPlayer;      // 当前行动玩家
    public int turnTimeLimit;      // 回合时间限制
    public List<Unit> units;       // 所有单位
}

public class TurnBasedLogic {
    // 回合开始
    public void onTurnStart(Room room, int playerId) {
        // 刷新行动点
        // 触发回合开始效果
        // 启动倒计时
    }
    
    // 移动单位
    public boolean moveUnit(Room room, long unitId, Position target) {
        // 检查行动点
        // 检查移动范围
        // 执行移动
        // 广播状态
    }
    
    // 攻击
    public boolean attack(Room room, long attackerId, long targetId) {
        // 检查攻击范围
        // 计算伤害
        // 应用伤害
        // 检查死亡
        // 广播状态
    }
    
    // 结束回合
    public void endTurn(Room room, int playerId) {
        // 切换到下一个玩家
        // 或进入下一回合
    }
}
```

## 扩展能力

### 服务器扩展策略

| 服务器 | 扩展方式 | 扩展依据 |
|--------|----------|----------|
| 网关 | 水平扩展 | 连接数 |
| 大厅 | 水平扩展 | 请求量 |
| 匹配 | 按模式分片 | 匹配队列长度 |
| 房间 | 水平扩展 | 房间数 |
| 聊天 | 按频道分片 | 消息量 |
| 全局服务 | 水平扩展 | 请求量 |

### 房间分配策略

```java
public class RoomAllocator {
    // 选择负载最低的房间服务器
    public RoomServer selectServer() {
        return roomServers.stream()
            .filter(s -> s.getRoomCount() < s.getMaxRooms())
            .min(Comparator.comparing(RoomServer::getRoomCount))
            .orElseThrow(() -> new NoAvailableServerException());
    }
}
```

## 断线重连

```
1. 玩家断线
2. 服务器保留房间状态 (5分钟)
3. 玩家重连
4. 验证身份
5. 同步完整房间状态
6. 继续游戏
```

```java
public class ReconnectHandler {
    public void onReconnect(Player player) {
        Room room = findPlayerRoom(player.id);
        if (room != null && room.status == PLAYING) {
            // 同步完整状态
            syncFullState(player, room);
            // 恢复玩家状态
            room.setPlayerOnline(player.id, true);
        }
    }
}
```

## 部署架构

### 最小部署 (开发/测试)

```
1 x 网关
1 x 大厅
1 x 匹配
1 x 房间
1 x 聊天
1 x Redis
1 x MySQL
```

### 生产部署 (1万在线)

```
3 x 网关 (每台 3000 连接)
2 x 大厅
1 x 匹配
5 x 房间 (每台 500 房间)
2 x 聊天
Redis 集群 (3主3从)
MySQL 主从 (1主2从)
```

### 大规模部署 (10万在线)

```
10 x 网关 (每台 10000 连接)
5 x 大厅
3 x 匹配 (按模式分片)
20 x 房间 (每台 1000 房间)
5 x 聊天 (按频道分片)
Redis 集群 (6主6从)
MySQL 集群 (分库分表)
```
