# Node Game Core - Node.js 游戏核心模块

Node.js 游戏服务器的核心框架，与 Java `game-core` 功能对应。

## 文件结构

```
node-game-core/
├── index.js             # 导出入口
├── lib/
│   ├── constants.js     # Cmd 和 ErrCode
│   ├── game-server.js   # GameServer 基类
│   ├── room-manager.js  # 房间管理
│   └── session-manager.js # 会话管理
└── package.json
```

## 导出

```javascript
const { GameServer, Cmd, ErrCode, RoomManager, SessionManager } = require('node-game-core');
```

## 核心类

### 常量 (constants.js)

```javascript
const Cmd = {
    // 系统
    LOGIN: 1,
    LOGIN_RESULT: 2,
    LOGOUT: 3,
    PING: 4,
    PONG: 5,
    
    // 通用游戏
    USER_JOIN: 500,
    USER_EXIT: 501,
    READY: 504,
    GAME_START: 510,
    GAME_OVER: 511,
    DEAL: 520,
    TURN: 522,
};

const ErrCode = {
    OK: 0,
    UNKNOWN: 1,
    NOT_LOGIN: 2,
    NOT_IN_ROOM: 3,
    ROOM_NOT_FOUND: 20,
    ROOM_FULL: 21,
    NOT_YOUR_TURN: 40,
};
```

### GameServer (game-server.js)

```javascript
class GameServer {
    constructor(config) { }
    
    // 必须重写
    getGameName() { throw new Error('Not implemented'); }
    startGame(room) { throw new Error('Not implemented'); }
    onGameCmd(userId, room, cmd, data) { throw new Error('Not implemented'); }
    
    // 可选重写
    onUserJoin(room, seat) { }
    onUserExit(room, seat) { }
    onGameOver(room) { }
    
    // 工具方法
    send(userId, cmd, data) { }
    sendError(userId, cmd, code, msg) { }
    broadcast(room, cmd, data) { }
    broadcastExcept(room, exceptUserId, cmd, data) { }
    
    // 启动
    start() { }
}
```

### RoomManager (room-manager.js)

```javascript
class RoomManager {
    createRoom(roomId, maxPlayers) { }
    getRoom(roomId) { }
    getRoomByUserId(userId) { }
    joinRoom(roomId, userId, name) { }
    exitRoom(userId) { }
    isAllReady(room) { }
    resetRoom(room) { }
    destroyRoom(roomId) { }
}
```

### Room 和 Seat

```javascript
// Room
{
    roomId: '123456',
    seats: [Seat, Seat, ...],
    game: null,           // 游戏状态
    status: 0,            // 0=等待, 1=游戏中
    createTime: Date.now()
}

// Seat
{
    seatIndex: 0,
    userId: 10001,
    name: '玩家名',
    ready: false,
    online: true
}
```

## 使用示例

```javascript
const { GameServer, Cmd } = require('node-game-core');

class MyGameServer extends GameServer {
    getGameName() {
        return '我的游戏';
    }
    
    startGame(room) {
        room.game = {
            currentSeat: 0,
            // ... 游戏状态
        };
        this.broadcast(room, Cmd.GAME_START, { firstSeat: 0 });
    }
    
    onGameCmd(userId, room, cmd, data) {
        const seatIndex = this.getSeatIndex(room, userId);
        
        switch (cmd) {
            case 1001: // 游戏操作
                if (room.game.currentSeat !== seatIndex) {
                    this.sendError(userId, cmd, 40, '不是你的回合');
                    return;
                }
                // 处理操作...
                this.broadcast(room, 1002, { result: 'ok' });
                break;
        }
    }
}

const yaml = require('js-yaml');
const fs = require('fs');
const config = yaml.load(fs.readFileSync('config.yml', 'utf8'));

new MyGameServer(config).start();
```

## 与 Java game-core 对应

| Node.js | Java |
|---------|------|
| `constants.js` | `Cmd.java` + `ErrCode.java` |
| `game-server.js` | `GameServer.java` |
| `room-manager.js` | `RoomMgr.java` |
| `session-manager.js` | (内置于 LiteJava) |
