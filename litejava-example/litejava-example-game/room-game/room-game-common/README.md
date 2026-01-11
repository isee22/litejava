# Room Game Common - 游戏公共模块

为所有游戏服务器提供基础设施，子类只需实现游戏特有逻辑。

## 核心类

```
room-game-common/
├── GameServer.java      # 游戏服务器基类 ★
├── RoomMgr.java         # 房间管理器 ★
├── TokenManager.java    # Token 管理
├── Cmd.java             # 协议命令号
├── ErrCode.java         # 错误码
├── GameException.java   # 业务异常
├── RoomSnapshot.java    # 断线重连快照
├── GameReplay.java      # 游戏回放
├── PlayerData.java      # 玩家数据记录
└── vo/                  # 数据对象
```

## GameServer - 游戏服务器基类

### 职责

```
┌─────────────────────────────────────────────────────────────┐
│                    GameServer<G>                             │
├─────────────────────────────────────────────────────────────┤
│  启动流程:                                                   │
│  1. 读取配置 (端口/gameType/hallUrl)                         │
│  2. 初始化 RoomMgr                                           │
│  3. 启动 WebSocket 服务                                      │
│  4. 注册 HTTP 接口                                           │
│  5. 向 HallServer 注册                                       │
│  6. 启动心跳                                                 │
├─────────────────────────────────────────────────────────────┤
│  HTTP 接口 (供 HallServer 调用):                             │
│  - /create_room      创建房间 (支持预生成房间号)             │
│  - /enter_room       进入房间 (返回 token)                   │
│  - /is_room_runing   检查房间是否存在                        │
│  - /get_server_info  获取服务器信息                          │
├─────────────────────────────────────────────────────────────┤
│  WebSocket 消息处理:                                         │
│  - LOGIN (1)         登录/进入房间                           │
│  - READY (413)       准备                                    │
│  - CHAT_SEND (300)   发送聊天                                │
│  - ROOM_EXIT (104)   退出房间                                │
│  - PING (4)          心跳                                    │
│  - 其他              交给子类 onGameCmd()                    │
├─────────────────────────────────────────────────────────────┤
│  子类必须实现:                                               │
│  - getGameName()     游戏名称                                │
│  - startGame(room)   开始游戏                                │
│  - onGameCmd(...)    处理游戏命令                            │
│  - getGameState(...) 断线重连状态 (可选)                     │
└─────────────────────────────────────────────────────────────┘
```

### 使用示例

```java
public class DoudizhuServer extends GameServer<DoudizhuGame> {
    
    @Override
    protected String getGameName() { 
        return "斗地主"; 
    }
    
    @Override
    protected void startGame(RoomMgr.Room<DoudizhuGame> room) {
        // 1. 创建游戏对象
        DoudizhuGame game = new DoudizhuGame(3);
        game.deal();
        room.game = game;
        
        // 2. 广播游戏开始
        broadcast(room, Cmd.GAME_START, new GameStartVO());
        
        // 3. 单播手牌
        for (RoomMgr.Seat seat : room.seats) {
            if (seat.userId > 0) {
                DealVO vo = new DealVO();
                vo.cards = game.getPlayerCards(seat.seatIndex);
                sendTo(seat.userId, Cmd.DEAL, vo);
            }
        }
    }
    
    @Override
    protected void onGameCmd(long userId, Room room, int cmd, Map data) {
        switch (cmd) {
            case DdzCmd.BID:  doBid(room, data);  break;
            case DdzCmd.PLAY: doPlay(room, data); break;
            case DdzCmd.PASS: doPass(room, data); break;
        }
    }
    
    @Override
    protected Object getGameState(Room room, int seatIndex) {
        // 断线重连时返回当前游戏状态
        if (room.game == null) return null;
        return new DdzStateVO(room.game, seatIndex);
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new DoudizhuServer().start(app);
    }
}
```

### 配置

```yaml
server:
  wsPort: 9100           # WebSocket 端口
  httpPort: 9101         # HTTP 端口
  clientip: localhost    # 客户端可访问的 IP
  clientport: 9100       # 客户端可访问的 WS 端口
  priKey: ROOM_PRI_KEY   # 签名密钥

game:
  type: doudizhu         # 游戏类型
  maxPlayers: 3          # 最大玩家数
  trusteeshipTimeout: 5000  # 托管超时 (毫秒)

hall:
  url: http://localhost:8201  # HallServer 地址
```

### 消息流程

```
WebSocket 消息 {cmd, data}
         │
         ▼
    onMessage()
         │
         ▼
    handleCmd()
         │
    ┌────┴────┬────────┬────────┬────────┐
    ▼         ▼        ▼        ▼        ▼
 LOGIN     READY    CHAT    EXIT     其他
    │         │        │        │        │
    ▼         ▼        ▼        ▼        ▼
onLogin() onReady() onChat() onExit() onGameCmd()
    │         │                          │
    │    检查全准备                       │
    │         │                          │
    │    startGame() ◄───────────────────┘
    │         │                    子类实现
    ▼         ▼
 绑定会话  广播状态
```

## RoomMgr - 房间管理器

### 数据结构

```
RoomMgr<G>
│
├── rooms: Map<String, Room<G>>
│   │
│   └── "123456" → Room
│                   ├── id: "123456"
│                   ├── ownerId: 1001
│                   ├── conf: {...}
│                   ├── seats: [Seat, Seat, Seat]
│                   │           │
│                   │           ├── seatIndex: 0
│                   │           ├── userId: 1001
│                   │           ├── name: "玩家1"
│                   │           ├── ready: true
│                   │           └── online: true
│                   │
│                   └── game: G (游戏状态，null=未开始)
│
└── userLocations: Map<Long, int[]>
    │
    └── 1001 → [123456, 0]  // userId → [roomId, seatIndex]
```

### API

```java
// 创建房间 (支持预生成房间号)
Room room = roomMgr.createRoom(userId, conf);
Room room = roomMgr.createRoom(userId, conf, "123456");

// 进入房间
int result = roomMgr.enterRoom(roomId, userId, name);
// 0=成功, 1=房间已满, 2=房间不存在

// 退出房间
roomMgr.exitRoom(userId);

// 准备
roomMgr.setReady(userId, true);

// 检查全准备
if (roomMgr.isAllReady(room)) {
    startGame(room);
}

// 查询
String roomId = roomMgr.getUserRoom(userId);
int seatIndex = roomMgr.getUserSeat(userId);
Room room = roomMgr.getRoom(roomId);
```

## TokenManager - Token 管理

参考 BabyKylin 的 tokenmgr.js 实现:

```java
// 创建 token (进入房间时)
String token = TokenManager.create(userId, roomId, 300000);

// 验证 token (WebSocket 登录时)
TokenInfo info = TokenManager.parse(token);
if (info.valid) {
    long userId = info.userId;
    String roomId = info.roomId;
}

// 标记已使用
TokenManager.markUsed(token);
```

## Cmd - 协议命令号

```java
// 系统 (1-99)
LOGIN = 1
PING = 4

// 房间 (100-199)
ROOM_EXIT = 104
USER_STATE = 113
USER_READY = 114

// 聊天 (300-399)
CHAT_SEND = 300
CHAT_MSG = 301

// 游戏通用 (400-499)
GAME_START = 400
GAME_OVER = 401
DEAL = 410
READY = 413

// 游戏特有 (1000+)
// 斗地主: 1001-1099
// 麻将:   1101-1199
```

## 签名验证

GameServer 与 HallServer 通信使用 MD5 签名:

```java
// 创建房间 (支持两种格式)
sign = md5(userId + roomId + conf + ROOM_PRI_KEY)  // 新格式
sign = md5(userId + conf + ROOM_PRI_KEY)           // 旧格式

// 进入房间
sign = md5(userId + name + roomId + ROOM_PRI_KEY)

// 检查房间
sign = md5(roomId + ROOM_PRI_KEY)
```

## 断线重连

```
1. 玩家断线 → onDisconnect()
   - 标记 seat.online = false
   - 记录 seat.disconnectTime
   - 广播 USER_STATE

2. 超过 trusteeshipTimeout → checkTrusteeship()
   - 标记 seat.trusteeship = true
   - 调用 onPlayerTrusteeship()
   - 子类可实现 AI 代打

3. 玩家重连 → onLogin()
   - 验证 token
   - 恢复 seat.online = true
   - 返回 getGameState() 恢复游戏状态
```

## 依赖关系

```
room-game-common
       │
       ├──► litejava-plugins (WebSocket, JSON, Config)
       │
       └──► 被以下模块依赖:
            ├── game-doudizhu
            ├── game-gobang
            ├── game-mahjong
            └── ...
```

注意: account-server 和 hall-server 不依赖此模块
