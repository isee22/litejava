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
