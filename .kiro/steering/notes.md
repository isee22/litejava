# LiteJava 开发注意事项

## 核心规则：配置文件优先

**这是 LiteJava 唯一的"魔法"规则，理解它就没有任何隐藏行为。**

### 配置优先级（从高到低）

1. **use() 后直接设置字段** - 最高优先级，强制覆盖
2. **配置文件** (application.yml / application.properties)
3. **代码构造参数**
4. **字段默认值**

### 为什么配置文件优先？

与 Spring Boot 一致，遵循"配置外部化"原则：
- 代码里写的是"默认值"
- 运维可以通过配置文件覆盖，无需改代码
- 便于不同环境（dev/test/prod）使用不同配置

### 示例

```java
// 场景 1：构造函数传参会被配置文件覆盖
app.use(new HttpServerPlugin(9000));  // 传入 9000
// 但配置文件有 server.port=8080
// 最终使用 8080（配置文件优先）

// 场景 2：如需代码强制指定，在 use() 后设置
HttpServerPlugin server = new HttpServerPlugin();
app.use(server);           // config() 在此执行，读取配置文件
server.port = 9000;        // 直接覆盖，最终使用 9000

// 场景 3：配置文件没有该配置项
app.use(new HttpServerPlugin(9000));  // 传入 9000
// 配置文件没有 server.port
// 最终使用 9000（构造参数作为默认值）
```

### 实现原理

插件的 `config()` 方法在 `app.use()` 时立即执行：

```java
public class ServerPlugin extends Plugin {
    public int port = 8080;  // 字段默认值
    
    public ServerPlugin(int port) {
        this.port = port;    // 构造参数覆盖默认值
    }
    
    @Override
    public void config() {
        // 配置文件有值则覆盖，没有则保持当前值
        port = app.conf.getInt("server", "port", port);
    }
}
```

执行顺序：
1. `new ServerPlugin(9000)` → port = 9000
2. `app.use(server)` → 调用 `config()`
3. `config()` 读取配置文件，有值则覆盖 → port = 8080
4. `server.port = 9000` → 直接覆盖 → port = 9000

---

## Java 8 兼容性

**litejava-core 和 litejava-plugins 目标是 JDK 1.8，不要使用 Java 9+ API**

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

---

## 插件开发规范

### 配置字段规范

1. 使用 public 字段，便于代码直接设置
2. 在 `config()` 开头集中加载所有配置
3. 字段名与配置 key 保持一致

```java
public class MyPlugin extends Plugin {
    // 配置字段（可代码设置，也可配置文件覆盖）
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

---

## 代码风格

- 使用 public 字段代替 getter/setter
- 优先使用 `Map<String, Object>` 传递数据
- 框架异常继承 `LiteJavaException`
- 不使用注解配置路由和中间件

---

## 模块 JDK 版本

| 模块 | JDK |
|------|-----|
| litejava-core | 1.8 |
| litejava-plugins | 1.8 |
| litejava-plugins-vt | 21 |
| litejava-examples | 1.8 |
| litejava-benchmark | 24 |
