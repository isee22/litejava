# 房间制游戏架构详解

## 适用场景

- 棋牌游戏 (斗地主、麻将、德州扑克)
- MOBA 游戏 (DOTA2、王者荣耀)
- FPS 游戏 (CS、PUBG)
- 战旗游戏 (火焰纹章、云顶之弈)
- 卡牌对战 (炉石传说、游戏王)

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

## 服务器架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          客户端                                  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       网关服务器 (Gateway)                       │
│  - 连接管理 (WebSocket/TCP)                                      │
│  - 协议解析                                                      │
│  - 路由转发                                                      │
│  - 心跳检测                                                      │
└─────────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
                ▼               ▼               ▼
┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐
│    大厅服务器      │ │    匹配服务器      │ │    房间服务器      │
│  - 登录/登出      │ │  - 匹配队列       │ │  - 房间管理       │
│  - 玩家信息      │ │  - 匹配算法       │ │  - 游戏逻辑       │
│  - 好友系统      │ │  - 房间分配       │ │  - 状态同步       │
└───────────────────┘ └───────────────────┘ └───────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         数据层                                   │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                         │
│  │  Redis  │  │  MySQL  │  │   MQ    │                         │
│  │ (缓存)  │  │ (持久化) │  │ (消息)  │                         │
│  └─────────┘  └─────────┘  └─────────┘                         │
└─────────────────────────────────────────────────────────────────┘
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

### 水平扩展

```bash
# 增加房间服务器
docker-compose up -d --scale room-server=5

# 每个房间服务器可承载 1000+ 房间
# 5 台服务器 = 5000+ 并发房间
```

### 房间分配策略

```java
public class RoomAllocator {
    // 选择负载最低的房间服务器
    public RoomServer selectServer() {
        return roomServers.stream()
            .min(Comparator.comparing(RoomServer::getRoomCount))
            .orElseThrow();
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
