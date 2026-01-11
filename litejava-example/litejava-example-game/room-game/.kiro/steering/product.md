# Product Overview

Room Game is a distributed multiplayer game server platform supporting various room-based games (Doudizhu/斗地主, Mahjong/麻将, Gobang/五子棋, Texas Hold'em, Niuniu/牛牛, Werewolf/狼人杀, MOBA).

## Core Features
- Real-time multiplayer gaming via WebSocket
- Player matchmaking system
- Room management (create, join, exit)
- Reconnection support with game state recovery
- AI trusteeship for disconnected players
- Game replay recording

## Architecture: BabyKylin Mode + Nginx 代理

采用 HTTP + WebSocket 分离架构，所有流量通过 Nginx 代理：

```
┌─────────────┐                    ┌─────────────┐
│   Client    │───── HTTP/WS ─────►│    Nginx    │
└─────────────┘                    └──────┬──────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
                    ▼                     ▼                     ▼
             ┌─────────────┐       ┌─────────────┐       ┌─────────────┐
             │AccountServer│       │ HallServer  │       │ GameServer  │
             │   (8101)    │       │   (8201)    │       │  (9100+)    │
             └─────────────┘       └─────────────┘       └─────────────┘
```

### 核心原则
- 客户端只与 Nginx 通信
- 进入游戏前用 HTTP（登录、创建房间、匹配）
- 进入游戏后用 WebSocket（通过 Nginx 代理到 GameServer）
- HallServer 返回游戏服 ip/port，客户端传给 Nginx，Nginx 代理到对应后端
