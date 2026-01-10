# 房间制游戏服务器

基于 LiteJava 的房间制游戏服务器示例。

## 适用游戏

- 棋牌 (斗地主、麻将)
- MOBA (类 DOTA2)
- 战旗 (回合制策略)
- 卡牌对战 (类炉石)

## 架构

```
┌─────────┐     ┌─────────┐     ┌─────────────────┐
│  客户端  │────▶│  网关    │────▶│   房间服务器     │
└─────────┘     └─────────┘     │  ┌─────┐ ┌─────┐│
                                │  │Room1│ │Room2││
                                │  └─────┘ └─────┘│
                                └─────────────────┘
```

## 模块结构

```
room-game/
├── src/main/java/game/
│   ├── RoomGameApp.java      # 启动类
│   ├── room/                 # 房间管理
│   │   ├── Room.java
│   │   ├── RoomManager.java
│   │   └── RoomState.java
│   ├── player/               # 玩家管理
│   │   ├── Player.java
│   │   └── PlayerManager.java
│   ├── match/                # 匹配系统
│   │   └── MatchService.java
│   ├── logic/                # 游戏逻辑
│   │   ├── GameLogic.java
│   │   └── CardGame.java     # 棋牌示例
│   └── net/                  # 网络层
│       ├── GameServer.java
│       └── MessageHandler.java
└── docs/
```

## 快速开始

```bash
mvn clean package -DskipTests
java -jar target/room-game.jar
```

## 配置

```yaml
server:
  port: 9000
  
room:
  maxRooms: 1000           # 最大房间数
  maxPlayersPerRoom: 10    # 每房间最大人数
  roomTimeout: 3600        # 房间超时 (秒)
```
