# Games Java - Java 游戏服务器

Java 语言实现的游戏服务器模块，包含核心框架和 7 个游戏实现。

## 模块结构

```
games-java/
├── game-core/           # 核心模块 (GameServer 基类、协议定义)
├── game-doudizhu/       # 斗地主 (3人)
├── game-gobang/         # 五子棋 (2人)
├── game-mahjong/        # 麻将 (4人)
├── game-texas/          # 德州扑克 (6人)
├── game-niuniu/         # 牛牛 (6人)
├── game-werewolf/       # 狼人杀 (8人)
└── game-moba/           # MOBA (10人)
```

## 游戏列表

| 游戏 | 人数 | WS 端口 | HTTP 端口 | 说明 |
|------|------|---------|-----------|------|
| game-doudizhu | 3 | 9100 | 9101 | 经典斗地主 |
| game-gobang | 2 | 9200 | 9201 | 五子棋 |
| game-mahjong | 4 | 9300 | 9301 | 四川麻将 |
| game-texas | 6 | 9400 | 9401 | 德州扑克 |
| game-niuniu | 6 | 9500 | 9501 | 牛牛 |
| game-werewolf | 8 | - | - | 狼人杀 |
| game-moba | 10 | - | - | MOBA |

## 开发新游戏

### 1. 创建模块

```
games-java/game-xxx/
├── pom.xml
├── README.md
└── src/main/
    ├── java/game/xxx/
    │   ├── XxxServer.java      # 继承 GameServer
    │   ├── XxxGame.java        # 游戏逻辑
    │   ├── XxxCmd.java         # 游戏命令号 (1000+)
    │   └── vo/                 # 游戏 VO
    └── resources/
        └── application.yml
```

### 2. 继承 GameServer

```java
public class XxxServer extends GameServer<XxxGame> {
    
    @Override
    protected String getGameName() {
        return "我的游戏";
    }
    
    @Override
    protected XxxGame createGame() {
        return new XxxGame();
    }
    
    @Override
    protected void startGame(Room<XxxGame> room) {
        // 初始化游戏状态
        room.getGame().init(room.getSeats());
        broadcast(room, Cmd.GAME_START, null);
    }
    
    @Override
    protected void onGameCmd(long userId, Room<XxxGame> room, int cmd, Map<String, Object> data) {
        // 处理游戏命令
        switch (cmd) {
            case XxxCmd.ACTION -> handleAction(userId, room, data);
        }
    }
    
    public static void main(String[] args) {
        new XxxServer().start();
    }
}
```

### 3. 配置文件

```yaml
server:
  id: xxx-1
  host: localhost
  wsPort: 9800
  httpPort: 9801

game:
  type: xxx
  maxPlayers: 4

registry:
  url: http://localhost:8201
```

### 4. pom.xml

```xml
<project>
    <parent>
        <groupId>litejava</groupId>
        <artifactId>games-java</artifactId>
        <version>1.0.0-jdk8</version>
    </parent>
    
    <artifactId>game-xxx</artifactId>
    
    <dependencies>
        <dependency>
            <groupId>litejava</groupId>
            <artifactId>game-core</artifactId>
        </dependency>
    </dependencies>
</project>
```

## 启动游戏服务

```bash
# 单个游戏
mvn exec:java -pl games-java/game-doudizhu -Dexec.mainClass=game.doudizhu.DoudizhuServer

# 多实例 (水平扩展)
mvn exec:java -pl games-java/game-doudizhu \
  -Dserver.id=ddz-2 -Dserver.wsPort=9102 -Dserver.httpPort=9103
```

## 协议命令号

| 范围 | 用途 |
|------|------|
| 1-99 | 系统 (LOGIN, LOGOUT, PING) |
| 100-149 | 房间操作 |
| 500-599 | 通用游戏 (READY, GAME_START, DEAL, TURN) |
| 1000+ | 游戏特定命令 |

详见 [game-core/README.md](game-core/README.md)
