# LiteJava 游戏服务器示例

基于 LiteJava 框架的游戏服务器架构示例，展示两种主流游戏架构模式。

## 项目结构

```
litejava-example-game/
├── game-common/          # 公共模块 (协议、工具)
├── room-game/            # 房间制游戏 (棋牌/MOBA/战旗)
├── mmorpg-game/          # MMORPG 游戏 (大世界)
└── docs/                 # 架构文档
    ├── ARCHITECTURE.md   # 总体架构对比
    ├── ROOM-GAME.md      # 房间制架构详解
    └── MMORPG-GAME.md    # MMORPG 架构详解
```

## 架构分类

### 房间制游戏 (Room-Based)

适用于：棋牌、MOBA、FPS、战旗、卡牌对战

```
┌─────────┐     ┌─────────┐     ┌─────────────────────────┐
│  客户端  │────▶│  网关    │────▶│      房间服务器集群       │
└─────────┘     └─────────┘     │  ┌─────┐ ┌─────┐ ┌─────┐│
                                │  │Room1│ │Room2│ │Room3││
                                │  └─────┘ └─────┘ └─────┘│
                                └─────────────────────────┘
```

特点：
- 房间隔离，互不影响
- 局内实时同步，局外无状态
- 水平扩展简单
- 断线重连容易

### MMORPG 游戏 (Massive Multiplayer)

适用于：魔兽世界、原神、传奇、仙侠

```
┌─────────┐     ┌─────────┐     ┌─────────────────────────┐
│  客户端  │────▶│  网关    │────▶│       场景服务器         │
└─────────┘     └─────────┘     │  ┌──────────────────┐  │
                                │  │   大世界地图       │  │
                                │  │  ┌────┐ ┌────┐   │  │
                                │  │  │AOI1│ │AOI2│   │  │
                                │  │  └────┘ └────┘   │  │
                                │  └──────────────────┘  │
                                └─────────────────────────┘
```

特点：
- 大世界持久状态
- AOI (Area of Interest) 同步
- 分线/跨服架构
- 复杂的状态管理

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | LiteJava |
| 网络 | Netty (TCP/WebSocket) |
| 协议 | Protobuf / JSON |
| 存储 | Redis + MySQL |
| 消息 | 内存队列 / RabbitMQ |

## 快速开始

```bash
# 构建
mvn clean package -DskipTests

# 启动房间制游戏服务器
cd room-game
java -jar target/room-game.jar

# 启动 MMORPG 服务器
cd mmorpg-game
java -jar target/mmorpg-game.jar
```

## 文档

- [架构对比](docs/ARCHITECTURE.md) - 两种架构的详细对比
- [房间制架构](docs/ROOM-GAME.md) - 棋牌/MOBA/战旗架构详解
- [MMORPG架构](docs/MMORPG-GAME.md) - 大世界游戏架构详解
