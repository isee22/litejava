# LiteJava 开发注意事项

## 配置优先级

**与 Spring Boot 同步的配置优先级**（从高到低）：

1. 命令行参数 `--server.port=8081`
2. 环境变量 `SERVER_PORT=8081`
3. 配置文件 (application.yml / application.properties)
4. 代码构造参数
5. 字段默认值

```java
// 代码设置端口 8080
app.use(new HttpServerPlugin(8080));

// 但配置文件写了 8081
// server:
//   port: 8081

// 最终使用 8081（配置文件优先）
```

**实现方式**：在 `config()` 方法中，使用当前字段值作为默认值：

```java
@Override
public void config() {
    // 配置文件有值则覆盖，没有则保持代码设置的值
    port = app.conf.getInt("server", "port", port);
}
```

## Java 8 兼容性

**不要使用 Java 9+ API**

litejava-core 和 litejava-plugins 目标是 JDK 1.8，以下 API 不可用：

| 不可用 | 替代方案 |
|--------|----------|
| `Map.of()` | `Maps.of()` (litejava.util.Maps) |
| `List.of()` | `Arrays.asList()` 或 `Collections.singletonList()` |
| `Set.of()` | `new HashSet<>(Arrays.asList(...))` |
| `String.isBlank()` | `str == null \|\| str.trim().isEmpty()` |
| `Optional.isEmpty()` | `!optional.isPresent()` |

```java
// ❌ 错误 - Java 9+
ctx.json(Map.of("msg", "Hello"));

// ✅ 正确 - Java 8
import litejava.util.Maps;
ctx.json(Maps.of("msg", "Hello"));
```

## 插件开发规范

### 配置字段

1. 使用 public 字段，便于代码直接设置
2. 在 `config()` 开头集中加载所有配置
3. 字段名与配置 key 保持一致

```java
public class MyPlugin extends Plugin {
    // 配置字段
    public int timeout = 30;
    public String endpoint;
    
    @Override
    public void config() {
        // 集中加载配置（配置文件优先）
        timeout = app.conf.getInt("my", "timeout", timeout);
        endpoint = app.conf.getString("my", "endpoint", endpoint);
        
        // 业务逻辑
        initClient();
    }
}
```

## 代码风格

- 使用 public 字段代替 getter/setter
- 优先使用 `Map<String, Object>` 传递数据
- 框架异常继承 `LiteJavaException`
- 不使用注解配置路由和中间件

## 模块 JDK 版本

| 模块 | JDK |
|------|-----|
| litejava-core | 1.8 |
| litejava-plugins | 1.8 |
| litejava-plugins-vt | 21 |
| litejava-examples | 1.8 |
| litejava-benchmark | 24 |
