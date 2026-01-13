# Project Structure

```
room-game/
├── account-server/          # 账号服务 (http:8101) - 登录/注册/玩家数据
├── hall-server/             # 大厅服务 (http:8201) - 房间调度/匹配/负载均衡
│
├── games-java/              # Java 游戏服务器 (Maven 多模块)
│   ├── game-core/           # 核心模块 (GameServer 基类、协议定义)
│   ├── game-doudizhu/       # 斗地主 3人 (ws:9100, http:9101)
│   ├── game-gobang/         # 五子棋 2人 (ws:9200, http:9201)
│   ├── game-mahjong/        # 麻将 4人 (ws:9300, http:9301)
│   ├── game-texas/          # 德州扑克 6人 (ws:9400, http:9401)
│   ├── game-niuniu/         # 牛牛 6人 (ws:9500, http:9501)
│   ├── game-werewolf/       # 狼人杀 8人
│   └── game-moba/           # MOBA 10人
│
├── games-go/                # Go 游戏服务器
│   ├── go-game-core/        # 核心模块
│   └── game-mahjong2/       # 二人麻将 2人 (ws:9600, http:9601)
│
├── games-node/              # Node.js 游戏服务器
│   ├── node-game-core/      # 核心模块
│   └── game-doudizhu4/      # 四人斗地主 4人 (ws:9700, http:9701)
│
├── robot-client/            # 机器人客户端 (测试用)
├── game-client/             # Web 客户端 (Vue 3, port:3000)
├── deploy/                  # 部署配置 (Nginx)
└── docs/                    # 协议文档
```

## 核心模块对应关系

| Java (game-core) | Go (go-game-core) | Node.js (node-game-core) |
|------------------|-------------------|--------------------------|
| `Cmd.java` | `cmd.go` | `lib/constants.js` |
| `ErrCode.java` | `cmd.go` | `lib/constants.js` |
| `GameServer.java` | `server.go` | `lib/game-server.js` |
| `RoomMgr.java` | `room.go` | `lib/room-manager.js` |

## 游戏模块模板

### Java (games-java/game-xxx/)
```
├── pom.xml                 # 依赖 game-core
├── README.md
└── src/main/
    ├── java/game/xxx/
    │   ├── XxxServer.java  # extends GameServer<XxxGame>
    │   ├── XxxGame.java    # 游戏逻辑
    │   ├── XxxCmd.java     # 命令号 (1000+)
    │   └── vo/             # 游戏 VO
    └── resources/
        └── application.yml
```

### Go (games-go/game-xxx/)
```
├── go.mod                  # require go-game-core
├── main.go                 # 入口
├── handler.go              # 消息处理
├── game_logic.go           # 游戏逻辑
└── config.yml
```

### Node.js (games-node/game-xxx/)
```
├── package.json            # 依赖 node-game-core
├── server.js               # extends GameServer
├── config.yml
└── src/
    ├── xxx-cmd.js          # 命令号
    └── game-logic.js       # 游戏逻辑
```

## Command Code Ranges
- 1-99: System (LOGIN, PING, etc.)
- 100-149: Room operations
- 150-199: Chat
- 200-299: Other lobby (sign-in, replay)
- 300-499: Matchmaking
- 500-599: Common game (USER_JOIN, GAME_START, DEAL, TURN)
- 1000+: Game-specific (each game defines its own)

## Configuration
Each service has `src/main/resources/application.yml`:
- `server.wsPort`, `server.httpPort`: Listening ports
- `server.clientip`: 对外地址，客户端通过这个地址连接（本地用 localhost，生产环境可能需要改成公网IP）
- `hall.url`: HallServer URL
- `game.type`, `game.maxPlayers`: Game-specific settings
