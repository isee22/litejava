# Project Structure

```
room-game/
├── room-game-common/        # Shared library (game servers depend on this)
│   └── game/common/
│       ├── Cmd.java         # Common command codes (1-999)
│       ├── ErrCode.java     # Error codes
│       ├── GameServer.java  # Base class for all game servers
│       ├── RoomMgr.java     # Room management
│       └── vo/              # Value objects (DTOs)
│
├── account-server/          # 账号服务 (http:8101) - 独立，无游戏依赖
│                            # 登录/注册/玩家数据/排行/商城/好友
├── hall-server/             # 大厅服务 (http:8201) - 独立，无游戏依赖
│                            # 房间创建/加入/匹配/GameServer 注册
│
├── game-doudizhu/           # Doudizhu game (ws:9100, http:9101)
├── game-gobang/             # Gobang game (ws:9200, http:9201)
├── game-mahjong/            # Mahjong game (ws:9300, http:9301)
├── game-texas/              # Texas Hold'em (ws:9400, http:9401)
├── game-niuniu/             # Niuniu game
├── game-werewolf/           # Werewolf game
├── game-moba/               # MOBA example (direct connect)
│
├── robot-client/            # AI robot client for testing
├── game-client/             # Vue 3 web client (port 3000)
└── docs/                    # Protocol documentation (Chinese)
```

## Game Server Module Pattern
Each game module follows this structure:
```
game-xxx/
├── pom.xml
├── README.md
└── src/main/
    ├── java/game/xxx/
    │   ├── XxxServer.java      # Extends GameServer<XxxGame>
    │   ├── XxxGame.java        # Game state and logic
    │   ├── XxxCmd.java         # Game-specific commands (1000+)
    │   ├── XxxErr.java         # Game-specific error codes
    │   ├── XxxAI.java          # AI logic (optional)
    │   ├── XxxScorer.java      # Scoring logic (optional)
    │   └── vo/                 # Game-specific VOs
    └── resources/
        └── application.yml     # Configuration
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
- `server.advertiseHost/Port`: External address for Docker/K8s
- `registry.url`: Registry server URL
- `game.type`, `game.maxPlayers`: Game-specific settings
