# MMORPG 游戏服务器

基于 LiteJava 的 MMORPG 游戏服务器示例。

## 适用游戏

- 传统 MMO (魔兽世界类)
- 开放世界 (原神类)
- 传奇类
- 仙侠武侠

## 架构

```
┌─────────┐     ┌─────────┐     ┌─────────────────┐
│  客户端  │────▶│  网关    │────▶│    场景服务器    │
└─────────┘     └─────────┘     │  ┌───────────┐  │
                                │  │  大世界    │  │
                                │  │  AOI 同步  │  │
                                │  └───────────┘  │
                                └─────────────────┘
```

## 模块结构

```
mmorpg-game/
├── src/main/java/game/
│   ├── MmorpgApp.java        # 启动类
│   ├── scene/                # 场景管理
│   │   ├── Scene.java
│   │   ├── SceneManager.java
│   │   └── AOIManager.java   # 视野管理
│   ├── entity/               # 实体
│   │   ├── Player.java
│   │   ├── Monster.java
│   │   └── Npc.java
│   ├── combat/               # 战斗系统
│   │   ├── CombatSystem.java
│   │   ├── SkillSystem.java
│   │   └── BuffSystem.java
│   ├── data/                 # 数据管理
│   │   ├── PlayerData.java
│   │   └── DataManager.java
│   └── net/                  # 网络层
│       ├── GameServer.java
│       └── MessageHandler.java
└── docs/
```

## 快速开始

```bash
mvn clean package -DskipTests
java -jar target/mmorpg-game.jar
```

## 配置

```yaml
server:
  port: 9000
  
scene:
  gridSize: 100            # AOI 格子大小
  syncInterval: 100        # 同步间隔 (ms)
  
database:
  redis:
    host: localhost
    port: 6379
  mysql:
    url: jdbc:mysql://localhost:3306/mmorpg
```
