# Product Overview

Room Game is a distributed multiplayer game server platform supporting various room-based games (Doudizhu/斗地主, Mahjong/麻将, Gobang/五子棋, Texas Hold'em, Niuniu/牛牛, Werewolf/狼人杀, MOBA).

## Core Features
- Real-time multiplayer gaming via WebSocket
- Player matchmaking system
- Room management (create, join, exit)
- Reconnection support with game state recovery
- AI trusteeship for disconnected players
- Game replay recording

## Architecture: HTTP + WebSocket 分离

采用 HTTP + WebSocket 分离架构，支持两种部署模式：

### 开发模式（直连）

```
┌─────────────────────────────────────────────────────────────────────┐
│                           客户端                                    │
│                                                                      │
│   HTTP 登录 ──────────────────────────► AccountServer (8101)        │
│   HTTP 匹配/创建房间 ─────────────────► HallServer (8201)           │
│   WebSocket 游戏 ─────────────────────► GameServer (9100+)          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

- Web 客户端: Vite 代理 HTTP 请求，WebSocket 直连 GameServer
- 机器人客户端: 直连各服务端口
- 无需 Nginx，启动服务即可开发

### 生产模式（Nginx 代理）

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

- 统一域名和端口
- SSL/TLS 终结
- 负载均衡
- 静态资源服务

### 核心原则
- 进入游戏前用 HTTP（登录、创建房间、匹配）
- 进入游戏后用 WebSocket（直连 GameServer）
- HallServer 返回 GameServer 的 ip:port，客户端直连
