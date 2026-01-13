# Game Core - 游戏核心模块

Java 游戏服务器的核心框架，提供 GameServer 基类、协议定义、房间管理等通用功能。

## 核心类

```
game.common/
├── GameServer.java      # 游戏服务器基类
├── RoomMgr.java         # 房间管理器
├── TokenManager.java    # Token 管理
├── Cmd.java             # 协议命令号
├── ErrCode.java         # 错误码
├── GameException.java   # 业务异常
├── GameReplay.java      # 游戏录像
├── PlayerData.java      # 玩家数据
├── RoomSnapshot.java    # 房间快照 (断线重连)
└── vo/                  # 通用 VO
```

## GameServer 基类

```java
public abstract class GameServer<T> {
    
    // 必须实现
    protected abstract String getGameName();
    protected abstract T createGame();
    protected abstract void startGame(Room<T> room);
    protected abstract void onGameCmd(long userId, Room<T> room, int cmd, Map<String, Object> data);
    
    // 可选重写
    protected void onUserJoin(Room<T> room, Seat seat) {}
    protected void onUserExit(Room<T> room, Seat seat) {}
    protected void onGameOver(Room<T> room) {}
    
    // 工具方法
    protected void send(long userId, int cmd, Object data);
    protected void send(long userId, int cmd, int code, Object data);
    protected void broadcast(Room<T> room, int cmd, Object data);
    protected void broadcastExcept(Room<T> room, long exceptUserId, int cmd, Object data);
}
```

## 协议命令号 (Cmd.java)

```java
public class Cmd {
    // 系统 (1-99)
    public static final int LOGIN = 1;
    public static final int LOGIN_RESULT = 2;
    public static final int LOGOUT = 3;
    public static final int PING = 4;
    public static final int PONG = 5;
    public static final int KICK = 10;
    
    // 房间 (100-149)
    public static final int ROOM_INFO = 100;
    
    // 通用游戏 (500-599)
    public static final int USER_JOIN = 500;
    public static final int USER_EXIT = 501;
    public static final int USER_OFFLINE = 502;
    public static final int USER_RECONNECT = 503;
    public static final int READY = 504;
    public static final int CANCEL_READY = 505;
    
    public static final int GAME_START = 510;
    public static final int GAME_OVER = 511;
    
    public static final int DEAL = 520;
    public static final int TURN = 522;
    
    // 游戏特定 (1000+)
    // 各游戏自定义
}
```

## 错误码 (ErrCode.java)

```java
public class ErrCode {
    public static final int OK = 0;
    public static final int UNKNOWN = 1;
    public static final int NOT_LOGIN = 2;
    public static final int NOT_IN_ROOM = 3;
    public static final int ALREADY_IN_MATCH = 4;
    public static final int NO_SERVER = 5;
    
    public static final int ROOM_NOT_FOUND = 20;
    public static final int ROOM_FULL = 21;
    public static final int ROOM_STARTED = 22;
    
    public static final int NOT_YOUR_TURN = 40;
    public static final int INVALID_ACTION = 41;
    public static final int INVALID_CARDS = 42;
}
```

## 房间管理 (RoomMgr.java)

```java
public class RoomMgr<T> {
    // 创建房间
    Room<T> createRoom(String roomId, int maxPlayers, Supplier<T> gameFactory);
    
    // 获取房间
    Room<T> getRoom(String roomId);
    Room<T> getRoomByUserId(long userId);
    
    // 玩家操作
    Seat joinRoom(String roomId, long userId, String name);
    void exitRoom(long userId);
    
    // 房间状态
    boolean isAllReady(Room<T> room);
    void resetRoom(Room<T> room);
    void destroyRoom(String roomId);
}
```

## Room 和 Seat

```java
public class Room<T> {
    private String roomId;
    private List<Seat> seats;
    private T game;              // 游戏状态
    private int status;          // 0=等待, 1=游戏中
    private long createTime;
}

public class Seat {
    private int seatIndex;
    private long userId;
    private String name;
    private boolean ready;
    private boolean online;
}
```

## 使用示例

```java
public class DoudizhuServer extends GameServer<DoudizhuGame> {
    
    @Override
    protected String getGameName() {
        return "斗地主";
    }
    
    @Override
    protected DoudizhuGame createGame() {
        return new DoudizhuGame();
    }
    
    @Override
    protected void startGame(Room<DoudizhuGame> room) {
        DoudizhuGame game = room.getGame();
        game.deal();  // 发牌
        
        // 广播游戏开始
        broadcast(room, Cmd.GAME_START, Map.of("bidSeat", game.getBidSeat()));
        
        // 单播发牌 (每人只能看到自己的牌)
        for (Seat seat : room.getSeats()) {
            send(seat.getUserId(), Cmd.DEAL, Map.of(
                "cards", game.getCards(seat.getSeatIndex())
            ));
        }
    }
    
    @Override
    protected void onGameCmd(long userId, Room<DoudizhuGame> room, int cmd, Map<String, Object> data) {
        DoudizhuGame game = room.getGame();
        int seatIndex = room.getSeatIndex(userId);
        
        switch (cmd) {
            case DoudizhuCmd.BID -> {
                boolean bid = (Boolean) data.get("bid");
                game.bid(seatIndex, bid);
                broadcast(room, DoudizhuCmd.BID_RESULT, game.getBidResult());
            }
            case DoudizhuCmd.PLAY -> {
                List<Integer> cards = (List<Integer>) data.get("cards");
                game.play(seatIndex, cards);
                broadcast(room, DoudizhuCmd.PLAY_RESULT, game.getPlayResult());
                
                if (game.isGameOver()) {
                    onGameOver(room);
                }
            }
        }
    }
}
```

## 与其他语言核心模块对应

| Java (game-core) | Go (go-game-core) | Node.js (node-game-core) |
|------------------|-------------------|--------------------------|
| `Cmd.java` | `cmd.go` | `lib/constants.js` |
| `ErrCode.java` | `cmd.go` | `lib/constants.js` |
| `GameServer.java` | `server.go` | `lib/game-server.js` |
| `RoomMgr.java` | `room.go` | `lib/room-manager.js` |
| `Room` / `Seat` | `Room` / `Seat` | `Room` / `Seat` |

三种语言的核心模块功能一致，协议兼容。
