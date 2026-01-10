# 游戏公共模块

游戏服务器公共代码，被 room-game 和 mmorpg-game 共同依赖。

## 内容

### 网络协议

```java
// 消息基类
public class GameMessage {
    public int cmd;        // 协议号
    public byte[] data;    // 数据
}

// 协议定义
public class Cmd {
    // 登录
    public static final int LOGIN_REQ = 1001;
    public static final int LOGIN_RES = 1002;
    
    // 房间
    public static final int JOIN_ROOM_REQ = 2001;
    public static final int JOIN_ROOM_RES = 2002;
    public static final int LEAVE_ROOM_REQ = 2003;
    
    // 游戏
    public static final int GAME_ACTION_REQ = 3001;
    public static final int GAME_STATE_SYNC = 3002;
}
```

### 实体基类

```java
public class Entity {
    public long id;
    public float x, y, z;
    public float rotation;
}

public class Player extends Entity {
    public String name;
    public int level;
    public long exp;
}
```

### 工具类

```java
// 定时器
public class GameTimer {
    public void schedule(Runnable task, long delay);
    public void scheduleAtFixedRate(Runnable task, long period);
}

// ID 生成器
public class IdGenerator {
    public long nextId();
}
```

## 使用

```xml
<dependency>
    <groupId>litejava</groupId>
    <artifactId>game-common</artifactId>
    <version>${project.version}</version>
</dependency>
```
