# Games Node - Node.js 游戏服务器

Node.js 实现的游戏服务器模块，展示多语言兼容性。

## 模块结构

```
games-node/
├── node-game-core/      # 核心模块
│   ├── index.js         # 导出入口
│   ├── lib/
│   │   ├── constants.js     # 协议常量
│   │   ├── game-server.js   # GameServer 基类
│   │   ├── room-manager.js  # 房间管理
│   │   └── session-manager.js # 会话管理
│   └── package.json
│
└── game-doudizhu4/      # 四人斗地主
    ├── server.js        # 入口 (继承 GameServer)
    ├── src/
    │   ├── doudizhu4-cmd.js  # 游戏命令
    │   └── game-logic.js     # 游戏逻辑
    ├── config.yml
    └── package.json
```

## 游戏列表

| 游戏 | 人数 | WS 端口 | HTTP 端口 |
|------|------|---------|-----------|
| game-doudizhu4 | 4 | 9700 | 9701 |

## 启动

```bash
cd games-node/game-doudizhu4
npm install
npm start
```

## 协议兼容

Node.js 游戏服务器与 Java 版本使用完全相同的协议：

- 向 HallServer 注册: `GET /register_gs`
- 心跳: `GET /heartbeat`
- 创建房间: `GET /create_room`
- 进入房间: `GET /enter_room`
- WebSocket 消息: `{cmd, code, data}`

## 开发新游戏

### 1. 创建目录

```
games-node/game-xxx/
├── package.json
├── server.js
├── config.yml
└── src/
    ├── xxx-cmd.js
    └── game-logic.js
```

### 2. package.json

```json
{
  "name": "game-xxx",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "start": "node server.js"
  },
  "dependencies": {
    "node-game-core": "file:../node-game-core",
    "js-yaml": "^4.1.0",
    "ws": "^8.14.0"
  }
}
```

### 3. 实现游戏服务器

```javascript
const { GameServer, Cmd } = require('node-game-core');
const XxxCmd = require('./src/xxx-cmd');
const GameLogic = require('./src/game-logic');

class XxxServer extends GameServer {
    getGameName() {
        return '我的游戏';
    }
    
    startGame(room) {
        room.game = new GameLogic();
        room.game.init(room.seats);
        this.broadcast(room, Cmd.GAME_START, null);
    }
    
    onGameCmd(userId, room, cmd, data) {
        switch (cmd) {
            case XxxCmd.ACTION:
                this.handleAction(userId, room, data);
                break;
        }
    }
}

const config = require('./config');
new XxxServer(config).start();
```

### 4. 配置文件

```yaml
server:
  id: xxx-1
  wsPort: 9800
  httpPort: 9801

game:
  type: xxx
  maxPlayers: 4

registry:
  url: http://localhost:8201
```
