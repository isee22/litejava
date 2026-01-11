# Room Game - BabyKylin 模式游戏服务器

基于 LiteJava 框架的游戏服务器，采用 BabyKylin 架构模式（HTTP + WebSocket 分离）。

## 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                            客户端                                    │
│                                                                      │
│   1. HTTP 登录 ──────────────────────────────────────────────────►  │
│   2. HTTP 创建/加入房间 ─────────────────────────────────────────►  │
│   3. 获取 token + wsUrl ◄────────────────────────────────────────   │
│   4. WebSocket 连接游戏 ─────────────────────────────────────────►  │
└───────────┬─────────────────────────────────────────────────────────┘
            │ 
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           Nginx (生产环境)                           │
│                                                                      │
│   /api/account/*  ──►  Account Server (8101)                        │
│   /api/hall/*     ──►  Hall Server (8201)                           │
│   /game/*         ──►  Game Server (9100+)                          │
└─────────────────────────────────────────────────────────────────────┘
            │                    │                    │
            ▼                    ▼                    ▼
      ┌──────────┐         ┌──────────┐        ┌──────────────┐
      │ Account  │         │   Hall   │◄───────│ GameServer 1 │
      │  Server  │         │  Server  │ 注册    ├──────────────┤
      │          │         │          │◄───────│ GameServer 2 │
      │ 8101     │         │ 8201     │ 心跳    ├──────────────┤
      └──────────┘         └──────────┘        │ GameServer N │
                                 │              └──────────────┘
                                 │
                           ┌─────┴─────┐
                           │  负载均衡  │
                           │  6位房间号 │
                           │  房间路由  │
                           └───────────┘
```

## 核心设计

### 1. 服务独立性

```
account-server  ──► litejava-plugins     (完全独立，无游戏依赖)
hall-server     ──► litejava-plugins     (完全独立，无游戏依赖)
game-xxx        ──► room-game-common ──► litejava-plugins
```

- **account-server**: 账号系统，与游戏逻辑完全解耦
- **hall-server**: 房间调度，只负责路由，不关心游戏细节
- **game-xxx**: 具体游戏，继承 GameServer 基类

### 2. HTTP vs WebSocket 分离

| 阶段 | 协议 | 服务 | 说明 |
|------|------|------|------|
| 登录 | HTTP | Account | 返回 session |
| 创建/加入房间 | HTTP | Hall | 返回 token + wsUrl |
| 匹配 | HTTP | Hall | 同步等待，返回 token |
| 游戏中 | WebSocket | Game | 实时双向通信 |

**原则**: 进入游戏前全部用 HTTP，进入游戏后用 WebSocket

### 3. 6位数字房间号

- 范围: 100000 - 999999
- 便于玩家口头分享
- 循环使用，自动检测冲突

### 4. 负载均衡

```
GameServer 启动 ──► 向 HallServer 注册 (clientip, clientport, httpPort, gameType)
                         │
每 5 秒心跳 ◄────────────┘ (上报 load = rooms * 10 + players)
                         │
创建房间时 ──► HallServer 选择 load 最低的 GameServer
```

## 服务列表

| 服务 | HTTP 端口 | WS 端口 | 职责 |
|------|-----------|---------|------|
| account-server | 8101 | - | 登录/注册/玩家数据/排行/商城/好友 |
| hall-server | 8201 | - | 房间创建/加入/匹配/GameServer 注册 |
| game-doudizhu | 9101 | 9100 | 斗地主游戏 |
| game-gobang | 9201 | 9200 | 五子棋游戏 |
| game-mahjong | 9301 | 9300 | 麻将游戏 |

## 完整流程

```
┌─────────┐      ┌─────────┐      ┌─────────┐      ┌─────────┐
│  客户端  │      │ Account │      │  Hall   │      │  Game   │
└────┬────┘      └────┬────┘      └────┬────┘      └────┬────┘
     │                │                │                │
     │ 1. POST /login │                │                │
     │───────────────>│                │                │
     │   {session}    │                │                │
     │<───────────────│                │                │
     │                │                │                │
     │ 2. GET /create_private_room     │                │
     │────────────────────────────────>│                │
     │                │                │ 3. 选择服务器   │
     │                │                │ 4. 生成房间号   │
     │                │                │ 5. POST /create_room
     │                │                │───────────────>│
     │                │                │   {roomid}     │
     │                │                │<───────────────│
     │                │                │ 6. POST /enter_room
     │                │                │───────────────>│
     │                │                │   {token}      │
     │                │                │<───────────────│
     │   {roomid, ip, port, token}     │                │
     │<────────────────────────────────│                │
     │                │                │                │
     │ 7. WebSocket ws://ip:port/game  │                │
     │─────────────────────────────────────────────────>│
     │ 8. LOGIN {token}                │                │
     │─────────────────────────────────────────────────>│
     │   验证 token，进入房间           │                │
     │<─────────────────────────────────────────────────│
     │                │                │                │
     │ 9. 游戏操作 (BID/PLAY/PASS...)  │                │
     │<────────────────────────────────────────────────>│
     │                │                │                │
```

## 快速启动

```bash
# Windows
start-all.cmd

# 或手动启动
mvn exec:java -pl account-server -Dexec.mainClass=game.account.AccountServer
mvn exec:java -pl hall-server -Dexec.mainClass=game.hall.HallServer
mvn exec:java -pl game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer
```

## API 参考

### Account Server (8101)

```bash
# 登录
POST /login
{"account": "test", "password": "123456"}
→ {"code": 0, "data": {"userId": 1001, "session": "xxx"}}

# 获取玩家信息
GET /player/{userId}
→ {"code": 0, "data": {"userId": 1001, "name": "玩家", "coins": 10000}}
```

### Hall Server (8201)

```bash
# 创建私人房间 (返回 6 位房间号)
GET /create_private_room?userId=1001&gameType=doudizhu&conf={}
→ {"code": 0, "data": {"roomid": "123456", "ip": "...", "port": 9100, "token": "xxx"}}

# 加入私人房间
GET /enter_private_room?userId=1002&roomId=123456&name=玩家2
→ {"code": 0, "data": {"roomid": "123456", "ip": "...", "port": 9100, "token": "xxx"}}

# 开始匹配
POST /match/start
{"userId": 1001, "gameType": "doudizhu", "level": "normal", "name": "玩家1"}
→ {"code": 0, "data": {"status": "matched", "roomid": "234567", "token": "xxx"}}
→ {"code": 0, "data": {"status": "matching"}}  // 等待中

# 取消匹配
POST /match/cancel
{"userId": 1001}

# 管理接口
GET /admin/servers      # 查看所有 GameServer
GET /admin/match_queues # 查看匹配队列
```

### Game Server (WebSocket)

```javascript
// 连接
ws = new WebSocket("ws://localhost:9100/game")

// 登录 (使用 Hall 返回的 token)
ws.send(JSON.stringify({cmd: 1, data: {token: "xxx"}}))

// 准备
ws.send(JSON.stringify({cmd: 413}))

// 游戏操作 (以斗地主为例)
ws.send(JSON.stringify({cmd: 1001, data: {score: 3}}))  // 叫地主
ws.send(JSON.stringify({cmd: 1002, data: {cards: [...]}}))  // 出牌
```

## Nginx 配置

```nginx
upstream account {
    server 127.0.0.1:8101;
}
upstream hall {
    server 127.0.0.1:8201;
}
upstream game {
    server 127.0.0.1:9100;
    server 127.0.0.1:9200;
    server 127.0.0.1:9300;
}

server {
    listen 80;
    
    location /api/account/ {
        proxy_pass http://account/;
    }
    
    location /api/hall/ {
        proxy_pass http://hall/;
    }
    
    location /game {
        proxy_pass http://game;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 目录结构

```
room-game/
├── account-server/      # 账号服务 (独立)
├── hall-server/         # 大厅服务 (独立)
├── room-game-common/    # 游戏公共模块
│   ├── GameServer.java  # 游戏服务器基类
│   ├── RoomMgr.java     # 房间管理器
│   ├── TokenManager.java # Token 管理
│   ├── Cmd.java         # 协议命令号
│   └── vo/              # 数据对象
├── game-doudizhu/       # 斗地主
├── game-gobang/         # 五子棋
├── game-mahjong/        # 麻将
├── game-client/         # Web 测试客户端
└── robot-client/        # 机器人客户端
```
