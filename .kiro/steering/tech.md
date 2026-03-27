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

## Exception Handling

### LiteJavaException 设计说明

框架异常包含两个 code：
- `statusCode`: HTTP 状态码 (200, 400, 404, 500) - 给浏览器/网关看
- `code`: 业务错误码 (40001, 41001) - 给前端业务逻辑看

### 使用规范

```java
// ✅ 业务异常 - HTTP 200 + 业务错误码
throw LiteJavaException.biz(Err.USER_NOT_FOUND, "用户不存在");
// 响应: HTTP 200, {code: 41001, msg: "用户不存在"}

// ✅ HTTP 异常 - 使用静态工厂方法
throw LiteJavaException.badRequest("参数错误");
throw LiteJavaException.notFound("资源不存在");
throw LiteJavaException.unauthorized("未登录");
// 响应: HTTP 404, {code: 404, msg: "资源不存在"}

// ❌ 不要直接用构造函数（容易混淆参数）
throw new LiteJavaException("错误", 404, 41001);  // statusCode 和 code 容易搞反
```

### RecoveryPlugin 错误处理

RecoveryPlugin 支持两种模式：

#### 1. JSON 模式（默认，适合 API）
```java
// 默认返回 JSON
app.use(new RecoveryPlugin());

// 响应: {code: 500, msg: "错误消息", data: null}
```

#### 2. 错误页面模式（适合传统 Web 应用）

**前提条件**：必须先安装模板引擎插件（ThymeleafPlugin 或 FreemarkerPlugin）

```java
// 1. 安装模板引擎
app.use(new ThymeleafPlugin());

// 2. 配置错误页面
RecoveryPlugin recovery = new RecoveryPlugin();
recovery.errorPageEnabled = true;
recovery.errorPages.put(404, "error/404");  // 404 错误页
recovery.errorPages.put(500, "error/500");  // 500 错误页
app.use(recovery);

// 3. 创建错误页面模板
// templates/error/404.html
// templates/error/500.html
```

**配置文件方式**：
```yaml
# application.yml
recovery:
  errorPageEnabled: true
  errorPage:
    404: error/404
    500: error/500
```

**自动判断**：RecoveryPlugin 会根据请求的 `Accept` 头自动选择：
- `Accept: application/json` → 返回 JSON
- `Accept: text/html` → 渲染错误页面（需要模板引擎）

**注意**：如果启用 `errorPageEnabled` 但没有配置模板引擎，会记录错误日志并降级为 JSON 响应。

### 业务错误码规范

每个项目应定义 `Err.java` 常量类：

```java
public class Err {
    // 5位数字：XXYYY
    // XX: 模块编号 (40=通用, 41=用户, 42=订单...)
    // YYY: 具体错误 (001-999)
    
    public static final int PARAM_REQUIRED = 40001;
    public static final int PARAM_INVALID = 40002;
    public static final int USER_NOT_FOUND = 41001;
    public static final int ORDER_NOT_FOUND = 42001;
}
```

## Coding Rules

### 1. 统一使用 LiteJavaException 静态方法
```java
// ❌ 错误 - 直接用构造函数容易混淆
throw new LiteJavaException("错误", 404, 41001);

// ✅ 正确 - 业务异常
throw LiteJavaException.biz(Err.USER_NOT_FOUND, "用户不存在");

// ✅ 正确 - HTTP 异常
throw LiteJavaException.notFound("资源不存在");
throw LiteJavaException.badRequest("参数错误");
```

### 2. Return 语句后不写代码
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

