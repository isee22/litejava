# Room Game - 分布式房间制游戏服务器

基于 LiteJava 框架的多语言游戏服务器平台，支持 Java、Go、Node.js 三种语言开发游戏玩法。

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              客户端                                          │
│                                                                              │
│   1. HTTP 登录/注册 ──────────────────────► AccountServer (8101)            │
│   2. HTTP 创建/加入房间 ─────────────────► HallServer (8201)                │
│   3. 获取 roomId + token + time + sign                                      │
│   4. WebSocket 连接 ─────────────────────► GameServer (9100+)               │
└───────────────────────────────────────────────────────────────────────────────┘

【开发模式】直连各服务端口，无需 Nginx
【生产模式】可选 Nginx 统一入口 (SSL/负载均衡)
```

## 核心设计原则

| 原则 | 说明 |
|------|------|
| HTTP/WS 分离 | 进入游戏前用 HTTP，进入游戏后用 WebSocket |
| 服务独立 | AccountServer、HallServer 不依赖游戏逻辑，可独立部署 |
| 多语言支持 | 游戏服务器可用 Java/Go/Node.js 开发，协议统一 |
| 水平扩展 | GameServer 可启动多实例，HallServer 自动负载均衡 |

## 目录结构

```
room-game/
├── account-server/          # 账号服务 (Java)
├── hall-server/             # 大厅服务 (Java)
│
├── games-java/              # Java 游戏服务器
│   ├── game-core/           #   核心模块 (GameServer 基类)
│   ├── game-doudizhu/       #   斗地主 (3人)
│   ├── game-gobang/         #   五子棋 (2人)
│   ├── game-mahjong/        #   麻将 (4人)
│   ├── game-texas/          #   德州扑克 (6人)
│   ├── game-niuniu/         #   牛牛 (6人)
│   ├── game-werewolf/       #   狼人杀 (8人)
│   └── game-moba/           #   MOBA (10人)
│
├── games-go/                # Go 游戏服务器
│   ├── go-game-core/        #   核心模块
│   └── game-mahjong2/       #   二人麻将 (2人)
│
├── games-node/              # Node.js 游戏服务器
│   ├── node-game-core/      #   核心模块
│   └── game-doudizhu4/      #   四人斗地主 (4人)
│
├── robot-client/            # 机器人客户端 (测试用)
├── game-client/             # Web 客户端 (Vue 3)
├── deploy/                  # 部署配置
└── docs/                    # 协议文档
```

## 服务端口

| 服务 | HTTP | WebSocket | 说明 |
|------|------|-----------|------|
| account-server | 8101 | - | 账号服务 |
| hall-server | 8201 | - | 大厅服务 |
| game-doudizhu | 9101 | 9100 | 斗地主 (Java) |
| game-gobang | 9201 | 9200 | 五子棋 (Java) |
| game-mahjong | 9301 | 9300 | 麻将 (Java) |
| game-texas | 9401 | 9400 | 德州扑克 (Java) |
| game-niuniu | 9501 | 9500 | 牛牛 (Java) |
| game-mahjong2 | 9601 | 9600 | 二人麻将 (Go) |
| game-doudizhu4 | 9701 | 9700 | 四人斗地主 (Node.js) |
| game-client | 3000 | - | Web 客户端 |

## 快速启动

### Windows 一键启动
```bash
start-all.cmd
```

### 手动启动
```bash
# 1. 编译
mvn clean package -DskipTests

# 2. 启动账号服务
mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer

# 3. 启动大厅服务
mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer

# 4. 启动游戏服务 (任选)
mvn exec:java -pl games-java/game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer

# 5. 启动客户端
cd game-client && npm install && npm run dev
```

### 启动 Go/Node.js 游戏服务
```bash
# Go 二人麻将
cd games-go/game-mahjong2 && go run .

# Node.js 四人斗地主
cd games-node/game-doudizhu4 && npm install && npm start
```

## 完整游戏流程

```
┌─────────┐      ┌─────────┐      ┌─────────┐      ┌─────────┐
│  客户端  │      │ Account │      │  Hall   │      │  Game   │
└────┬────┘      └────┬────┘      └────┬────┘      └────┬────┘
     │                │                │                │
     │ 1. POST /login │                │                │
     │───────────────►│                │                │
     │   {userId, roomId?}             │                │  ← roomId 用于断线重连
     │◄───────────────│                │                │
     │                │                │                │
     │ 2. POST /create_room 或 /enter_room             │
     │────────────────────────────────►│                │
     │                │                │ 3. 选择服务器   │
     │                │                │ 4. 调用 GameServer
     │                │                │───────────────►│
     │   {roomId, ip, port, token, time, sign}         │  ← 带签名
     │◄────────────────────────────────│                │
     │                │                │                │
     │ 5. WebSocket ws://ip:port/game  │                │
     │─────────────────────────────────────────────────►│
     │ 6. LOGIN {token, roomid, time, sign}            │  ← 带签名
     │─────────────────────────────────────────────────►│
     │   验证签名 + token，进入房间     │                │
     │◄─────────────────────────────────────────────────│
     │                │                │                │
     │ 7. 游戏操作 (READY/BID/PLAY...) │                │
     │◄────────────────────────────────────────────────►│
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | LiteJava (自研轻量框架) |
| 后端语言 | Java 21 / Go 1.21+ / Node.js 18+ |
| 数据库 | MySQL (仅 AccountServer) |
| 缓存 | Redis (可选，用于集群) |
| 通信协议 | HTTP + WebSocket + JSON |
| 前端 | Vue 3 + Vite 5 + Pinia |
| 构建 | Maven (Java) / Go Modules / npm |

## 文档

- [客户端对接文档](docs/客户端对接文档.md)
- [游戏服务器协议](docs/游戏服务器协议.md)
- [部署指南-本地](docs/部署指南-本地.md)
- [部署指南-Docker](docs/部署指南-Docker.md)
- [部署指南-Nginx](docs/部署指南-Nginx.md)

## License

MIT
