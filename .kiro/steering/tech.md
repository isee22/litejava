# Tech Stack

## Build System

- Maven multi-module project
- JDK 1.8 (Java 8)

## Project Modules

- `litejava-core` - Zero external dependencies, pure JDK implementation
- `litejava-plugins` - Optional official plugins with external dependencies

## Testing

- JUnit 5 for unit tests
- jqwik for property-based testing

## Key Dependencies

Core module: None (pure JDK)

Plugins module:
- JDBC drivers for database plugin
- Redis client for cache plugin

## Common Commands

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Run specific module tests
mvn test -pl litejava-core

# Package without tests
mvn package -DskipTests
```

## Code Style

- Use public fields instead of getters/setters
- Prefer `Map<String, Object>` over POJOs for data transfer
- Use functional interfaces and lambdas
- No annotations for configuration
- All framework exceptions extend `LiteJavaException`

## Coding Rules

### 1. Return 语句后不写代码
```java
// ❌ 错误
if (user == null) {
    ctx.fail(404, "用户不存在");
    return;
}
// 后续代码...

// ✅ 正确 - return 后直接结束，不要有多余代码
if (user == null) {
    return ctx.fail(404, "用户不存在");
}
```

### 2. 使用 app.log 替代 System.out.println
```java
// ❌ 错误
System.out.println("启动成功");

// ✅ 正确
app.log.info("启动成功");
```

### 3. Controller 统一响应格式
使用 `ctx.ok(data)` 和 `ctx.fail(code, msg)` 替代手动构造响应：
```java
// ❌ 错误
ctx.json(Map.of("code", 0, "msg", "success", "data", order));

// ✅ 正确
ctx.ok(order);
ctx.fail(400, "参数错误");
```

### 4. HTTP 调用使用 RpcClient
服务间调用使用 `RpcClient`（基于 OkHttp 连接池），不要直接用 `HttpURLConnection`：
```java
// ❌ 错误
HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

// ✅ 正确
RpcClient rpc = app.getPlugin(RpcClient.class);
Map<String, Object> result = rpc.call("user-service", "/user/info", body);
```

### 5. 方法参数不能嵌套方法调用
方法调用的参数必须先赋值给变量，不能直接嵌套另一个方法调用：
```java
// ❌ 错误 - 嵌套调用不易调试
ctx.ok(UserService.create(user));
ctx.ok(ListResult.of(UserService.findAll()));

// ✅ 正确 - 先赋值再调用
User created = UserService.create(user);
ctx.ok(created);

List<User> users = UserService.findAll();
ctx.ok(ListResult.of(users));
```

## MVC 分层架构

采用经典 MVC 分层，各层职责明确：

### 层级职责

| 层级 | 职责 | 依赖 |
|------|------|------|
| Controller | 接收请求、参数校验、调用 Service、返回响应 | Service |
| Service | 业务逻辑、事务控制、调用 DAO | DAO |
| DAO | 数据访问、操作缓存和 Mapper | Cache, Mapper |

### 调用规则

```
Controller → Service → DAO → Cache / Mapper (MyBatis)
     ↓           ↓         ↓
    VO          PO        PO
```

- Controller 只能调用 Service，不能直接操作 DAO
- Service 只能调用 DAO，不能直接操作 Cache/Mapper
- DAO 负责操作缓存 (Redis) 和数据库 (MyBatis Mapper)

### 数据对象规范

| 类型 | 说明 | 位置 | 命名规则 |
|------|------|------|----------|
| Entity | 数据库表映射对象 | `entity/` | 不带后缀，如 `User` |
| VO (View Object) | 对外返回的视图对象 | `vo/` | 带 VO 后缀，如 `UserVO` |

```java
// Entity - 对应数据库表，不带后缀
public class User {
    public Long id;
    public String username;
    public String password;  // 敏感字段
    public Date createTime;
}

// VO - 对外返回，带 VO 后缀
public class UserVO {
    public Long id;
    public String username;
    public String nickname;
    // 不暴露 password
    
    // VO 可以包含 Entity 或从 Entity 转换
    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.id = user.id;
        vo.username = user.username;
        return vo;
    }
}
```

### 包结构示例

```
com.example.user/
├── controller/
│   └── UserController.java
├── service/
│   └── UserService.java
├── dao/
│   └── UserDAO.java          # DAO 内部直接操作缓存和 Mapper
├── mapper/
│   └── UserMapper.java
├── entity/
│   └── User.java
└── vo/
    └── UserVO.java
```

> DAO 直接集成缓存操作，无需单独 cache 包

### 代码示例

```java
// Controller - 只调用 Service
public class UserController {
    private UserService userService;
    
    public void getUser(Context ctx) {
        Long userId = ctx.pathLong("id");
        UserVO user = userService.getUserById(userId);
        if (user == null) {
            return ctx.fail(404, "用户不存在");
        }
        ctx.ok(user);
    }
}

// Service - 只调用 DAO，处理业务逻辑
public class UserService {
    private UserDAO userDAO;
    
    public UserVO getUserById(Long id) {
        User user = userDAO.findById(id);
        if (user == null) {
            return null;
        }
        return UserVO.from(user);
    }
}

// DAO - 操作缓存和 Mapper
public class UserDAO {
    private RedisCachePlugin cache;
    private UserMapper mapper;
    
    public User findById(Long id) {
        // 先查缓存
        String key = "user:" + id;
        User user = cache.get(key, User.class);
        if (user != null) {
            return user;
        }
        // 缓存未命中，查数据库
        user = mapper.selectById(id);
        if (user != null) {
            cache.set(key, user, 3600);
        }
        return user;
    }
}
```

## room-game 架构 (BabyKylin 模式)

采用 HTTP + WebSocket 分离架构：
- 进入游戏前：全部用 HTTP (登录/匹配/房间操作)
- 进入游戏后：用 WebSocket (游戏操作)

生产环境用 Nginx 做反向代理，隐藏内部服务 IP。

### CMD 定义 (仅用于 WebSocket 游戏通信)

| 范围 | 说明 |
|------|------|
| 1-99 | 系统 (LOGIN, PING 等) |
| 100-499 | 房间/聊天 |
| 500-999 | 游戏通用 |
| 1000+ | 各游戏自定义 |

新增 CMD 时在 `room-game-common/Cmd.java` 定义常量。

