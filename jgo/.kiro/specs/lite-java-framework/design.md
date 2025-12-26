# Design Document

## Overview

LiteJava是一个反传统的Java Web框架，基于JDK 1.8，借鉴Koa 3.0的中间件设计和Go语言的简洁风格。框架核心目标是让Java开发像Go一样直接，代码即文档，所见即所得。

### 设计原则

1. **No Annotations** - 所有配置通过代码完成，拒绝魔法
2. **No Getter/Setter** - 使用public字段或Map，直接访问
3. **No Interfaces** - 使用函数式接口和Lambda，不强制实现接口
4. **Explicit over Implicit** - 显式优于隐式，无隐藏规则
5. **Composition over Inheritance** - 组合优于继承，通过中间件组合功能

### 使用示例

```java
public class Main {
    public static void main(String[] args) {
        App app = new App();
        
        // 中间件
        app.use((ctx, next) -> {
            long start = System.currentTimeMillis();
            next.run();
            System.out.println("Request took: " + (System.currentTimeMillis() - start) + "ms");
        });
        
        // 路由
        app.get("/", ctx -> ctx.text("Hello LiteJava!"));
        app.get("/users/:id", ctx -> {
            String id = ctx.param("id");
            ctx.json(Map.of("id", id, "name", "John"));
        });
        
        // 启动
        app.listen(8080);
    }
}
```

## Architecture

### 模块划分

项目采用核心与插件分离的架构，分为两个独立的Maven模块：

```
litejava/
├── litejava-core/          # 核心模块 - 零依赖，纯JDK实现
│   ├── pom.xml
│   └── src/main/java/
│       └── io/litejava/
│           ├── App.java
│           ├── Context.java
│           ├── Router.java
│           ├── Middleware.java
│           ├── Handler.java
│           ├── Plugin.java          # 插件契约接口
│           ├── Config.java          # 统一配置系统
│           ├── Json.java            # 内置轻量JSON处理
│           └── exception/
│
└── litejava-plugins/       # 官方插件模块 - 可选依赖
    ├── pom.xml
    └── src/main/java/
        └── io/litejava/plugins/
            ├── database/
            ├── cache/
            ├── websocket/
            └── ssl/
```

### 核心模块设计原则

1. **零外部依赖** - 核心模块只依赖JDK 1.8，不引入任何第三方库
2. **插件契约** - 定义清晰的Plugin接口，任何人都可以实现自己的插件
3. **可替换性** - 官方插件只是参考实现，用户可以用自己的实现替换

### 插件契约

```java
// 核心模块定义的插件契约
@FunctionalInterface
public interface Plugin {
    /**
     * 初始化插件
     * @param config 配置参数，使用Map避免强类型依赖
     * @return 插件实例，用户通过ctx.plugin(name)获取
     */
    Object init(Map<String, Object> config) throws Exception;
    
    /**
     * 可选：插件销毁时调用
     */
    default void destroy() {}
    
    /**
     * 可选：插件描述信息
     */
    default String description() { return ""; }
}
```

### 用户自定义插件示例

```java
// 用户可以轻松实现自己的插件
public class MyRedisPlugin implements Plugin {
    @Override
    public Object init(Map<String, Object> config) {
        String host = (String) config.getOrDefault("host", "localhost");
        int port = (int) config.getOrDefault("port", 6379);
        return new MyRedisClient(host, port);
    }
}

// 使用自定义插件
app.plugin("redis", new MyRedisPlugin(), Map.of("host", "127.0.0.1"));

// 在Handler中使用
app.get("/data", ctx -> {
    MyRedisClient redis = ctx.plugin("redis");
    ctx.json(redis.get("key"));
});
```

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              用户应用                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    litejava-core (核心模块)                      │   │
│  │  ┌─────────┐  ┌─────────────┐  ┌─────────────────────────────┐  │   │
│  │  │   App   │  │   Router    │  │     Plugin Manager          │  │   │
│  │  └─────────┘  └─────────────┘  │  ┌─────────────────────┐    │  │   │
│  │  ┌─────────┐  ┌─────────────┐  │  │  Plugin Interface   │◄───┼──┼───┤
│  │  │ Context │  │ Middleware  │  │  │  (契约/接口)        │    │  │   │
│  │  └─────────┘  │   Chain     │  │  └─────────────────────┘    │  │   │
│  │  ┌─────────┐  └─────────────┘  └─────────────────────────────┘  │   │
│  │  │  Json   │  ┌─────────────┐                                   │   │
│  │  │ (内置)  │  │ HTTP Server │                                   │   │
│  │  └─────────┘  └─────────────┘                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    ▲                                    │
│                                    │ 实现                               │
│  ┌─────────────────────────────────┴───────────────────────────────┐   │
│  │                litejava-plugins (官方插件，可选)                  │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │   │
│  │  │ Database │  │  Cache   │  │WebSocket │  │ 用户自定义插件    │ │   │
│  │  │  Plugin  │  │  Plugin  │  │  Plugin  │  │ (可替换官方实现)  │ │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Maven依赖关系

```xml
<!-- 用户只需要核心模块即可运行 -->
<dependency>
    <groupId>io.litejava</groupId>
    <artifactId>litejava-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 官方插件是可选的 -->
<dependency>
    <groupId>io.litejava</groupId>
    <artifactId>litejava-plugins</artifactId>
    <version>1.0.0</version>
    <optional>true</optional>
</dependency>
```

### 中间件洋葱模型

```
Request  ──────────────────────────────────────────►
         │                                         │
         ▼                                         │
    ┌─────────┐                                    │
    │  MW 1   │ ─── before next ───►               │
    │         │                    │               │
    │         │ ◄── after next ────┤               │
    └─────────┘                    │               │
         │                         ▼               │
         │                    ┌─────────┐          │
         │                    │  MW 2   │ ──►      │
         │                    │         │    │     │
         │                    │         │ ◄──┤     │
         │                    └─────────┘    │     │
         │                         │         ▼     │
         │                         │    ┌─────────┐│
         │                         │    │ Handler ││
         │                         │    └─────────┘│
         │                         │               │
◄────────┴─────────────────────────┴───────────────┘
Response
```

## Components and Interfaces

### 1. App - 应用主类

```java
public class App {
    public int port = 8080;
    public boolean devMode = true;
    
    // 中间件注册
    public App use(Middleware middleware);
    
    // 路由注册
    public App get(String path, Handler handler);
    public App post(String path, Handler handler);
    public App put(String path, Handler handler);
    public App delete(String path, Handler handler);
    public App route(String method, String path, Handler handler);
    
    // 路由分组
    public Router group(String prefix);
    
    // 插件注册
    public App plugin(String name, Plugin plugin);
    public App plugin(String name, Plugin plugin, Map<String, Object> config);
    
    // 静态文件
    public App staticFiles(String urlPath, String directory);
    
    // WebSocket
    public App ws(String path, WebSocketHandler handler);
    
    // SSL配置
    public App ssl(String certPath, String keyPath);
    
    // 启动服务器
    public void listen(int port);
    public void listen(); // 使用默认端口
}
```

### 2. Context - 请求上下文

```java
public class Context {
    // 请求信息 (public字段，直接访问)
    public String method;
    public String path;
    public String query;
    public Map<String, String> headers;
    public Map<String, String> params;      // 路径参数
    public Map<String, String> queryParams; // 查询参数
    
    // 请求体读取
    public String bodyString();
    public byte[] bodyBytes();
    public Map<String, Object> bodyJson();
    public List<Object> bodyJsonArray();
    public Map<String, String> bodyForm();
    public Map<String, UploadedFile> bodyFiles();
    
    // 响应设置
    public Context status(int code);
    public Context header(String name, String value);
    public Context body(String content);
    public Context body(byte[] content);
    public Context json(Object obj);
    public Context html(String content);
    public Context redirect(String url);
    public Context file(String path);
    
    // 插件访问
    public <T> T plugin(String name);
    
    // 存储请求级别数据
    public Map<String, Object> state;
}
```

### 3. Middleware - 中间件函数式接口

```java
@FunctionalInterface
public interface Middleware {
    void handle(Context ctx, Next next) throws Exception;
}

@FunctionalInterface
public interface Next {
    void run() throws Exception;
}
```

### 4. Handler - 请求处理器

```java
@FunctionalInterface
public interface Handler {
    void handle(Context ctx) throws Exception;
}
```

### 5. Router - 路由器

```java
public class Router {
    public String prefix;
    
    public Router get(String path, Handler handler);
    public Router post(String path, Handler handler);
    public Router put(String path, Handler handler);
    public Router delete(String path, Handler handler);
    public Router use(Middleware middleware);
    public Router group(String prefix);
}
```

### 6. Plugin - 插件契约（核心模块定义）

```java
/**
 * 插件契约接口 - 定义在核心模块中
 * 任何人都可以实现此接口来创建自定义插件
 */
@FunctionalInterface
public interface Plugin {
    /**
     * 初始化插件并返回插件实例
     * @param config 配置参数，使用Map保持灵活性
     * @return 插件实例，通过ctx.plugin(name)获取
     */
    Object init(Map<String, Object> config) throws Exception;
    
    /**
     * 插件销毁时调用，用于清理资源
     */
    default void destroy() {}
    
    /**
     * 插件描述信息
     */
    default String description() { return ""; }
}

/**
 * 插件生命周期钩子（可选实现）
 */
public interface PluginLifecycle {
    void onStart(App app);
    void onStop(App app);
}
```

### 7. 官方插件（litejava-plugins模块，可替换）

官方插件只是参考实现，用户可以用自己的实现完全替换。

#### DatabasePlugin - 数据库插件

```java
// 官方实现 - 用户可以替换为自己的实现
public class DatabasePlugin implements Plugin {
    @Override
    public Object init(Map<String, Object> config) {
        return new Database(config);
    }
}

// 数据库操作接口 - 用户可以实现自己的版本
public class Database {
    public List<Map<String, Object>> query(String sql, Object... params);
    public int execute(String sql, Object... params);
    public void transaction(TransactionCallback callback);
    public void close();
}

// 用户自定义数据库插件示例
public class MyDatabasePlugin implements Plugin {
    @Override
    public Object init(Map<String, Object> config) {
        // 使用自己喜欢的连接池或ORM
        return new MyCustomDatabase(config);
    }
}
```

#### CachePlugin - 缓存插件

```java
// 官方提供两种实现，用户也可以自己实现
public class MemoryCachePlugin implements Plugin {
    @Override
    public Object init(Map<String, Object> config) {
        return new MemoryCache(config);
    }
}

public class RedisCachePlugin implements Plugin {
    @Override
    public Object init(Map<String, Object> config) {
        return new RedisCache(config);
    }
}

// 缓存操作接口
public class Cache {
    public void set(String key, Object value);
    public void set(String key, Object value, long ttlSeconds);
    public <T> T get(String key);
    public boolean delete(String key);
    public boolean exists(String key);
}

// 用户自定义缓存插件示例
public class MyCaffeinePlugin implements Plugin {
    @Override
    public Object init(Map<String, Object> config) {
        // 使用Caffeine作为缓存实现
        return new CaffeineCache(config);
    }
}
```

#### 插件使用示例

```java
// 使用官方插件
app.plugin("db", new DatabasePlugin(), Map.of(
    "url", "jdbc:mysql://localhost:3306/test",
    "user", "root",
    "password", "123456"
));

// 使用自定义插件替换官方实现
app.plugin("db", new MyDatabasePlugin(), Map.of(...));

// 在Handler中使用 - 无论用哪个实现，使用方式一致
app.get("/users", ctx -> {
    Database db = ctx.plugin("db");
    ctx.json(db.query("SELECT * FROM users"));
});
```

### 8. WebSocket处理

```java
public class WebSocketHandler {
    public Consumer<WebSocketSession> onOpen;
    public BiConsumer<WebSocketSession, String> onMessage;
    public Consumer<WebSocketSession> onClose;
    public BiConsumer<WebSocketSession, Throwable> onError;
}

public class WebSocketSession {
    public String id;
    public void send(String message);
    public void close();
    public Map<String, Object> state;
}
```

### 9. Config - 统一配置系统（核心模块）

配置系统是核心模块的一部分，提供统一的配置管理能力。

```java
/**
 * 统一配置类 - 支持多种格式和来源
 */
public class Config {
    // 加载配置
    public static Config load();                           // 自动加载 config.yml/json/properties
    public static Config load(String path);                // 指定文件路径
    public static Config load(String path, String profile); // 指定文件和环境
    
    // 类型安全的值获取
    public String get(String key);
    public String get(String key, String defaultValue);
    public int getInt(String key);
    public int getInt(String key, int defaultValue);
    public boolean getBool(String key);
    public boolean getBool(String key, boolean defaultValue);
    public long getLong(String key);
    public double getDouble(String key);
    public List<String> getList(String key);
    public Map<String, Object> getMap(String key);
    
    // 必需值（缺失时抛异常）
    public String require(String key);
    public int requireInt(String key);
    
    // 获取子配置（用于插件命名空间）
    public Config sub(String prefix);
    
    // 原始Map访问
    public Map<String, Object> toMap();
}
```

#### 配置文件格式支持

```yaml
# config.yml - 主配置文件
app:
  name: MyApp
  port: 8080
  devMode: true

# 数据库配置（插件命名空间）
database:
  url: jdbc:mysql://localhost:3306/mydb
  user: root
  password: ${DB_PASSWORD}  # 环境变量占位符
  pool:
    maxSize: 10
    minIdle: 2

# 缓存配置
cache:
  type: redis
  host: ${REDIS_HOST:localhost}  # 带默认值的占位符
  port: 6379

# SSL配置
ssl:
  enabled: false
  cert: /path/to/cert.pem
  key: /path/to/key.pem
```

```yaml
# config-prod.yml - 生产环境配置（覆盖主配置）
app:
  devMode: false
  port: 80

database:
  pool:
    maxSize: 50
```

#### 配置加载优先级

```
低优先级 ──────────────────────────────────────► 高优先级

config.yml  →  config-{profile}.yml  →  环境变量  →  代码设置
   (基础)         (环境覆盖)           (运行时)      (显式)
```

#### 使用示例

```java
public class Main {
    public static void main(String[] args) {
        // 加载配置（自动检测环境）
        Config config = Config.load();
        
        // 或指定环境
        Config config = Config.load("config.yml", "prod");
        
        App app = new App();
        
        // 从配置读取应用设置
        app.port = config.getInt("app.port", 8080);
        app.devMode = config.getBool("app.devMode", true);
        
        // 插件使用子配置
        app.plugin("db", new DatabasePlugin(), config.sub("database").toMap());
        app.plugin("cache", new RedisCachePlugin(), config.sub("cache").toMap());
        
        // SSL配置
        if (config.getBool("ssl.enabled", false)) {
            app.ssl(config.get("ssl.cert"), config.get("ssl.key"));
        }
        
        app.get("/", ctx -> ctx.json(Map.of("name", config.get("app.name"))));
        app.listen();
    }
}
```

#### 配置验证

```java
// 启动时验证必需配置
Config config = Config.load();
config.require("database.url");      // 缺失时抛出 ConfigException
config.require("database.user");
config.requireInt("app.port");

// 批量验证
config.requireAll("database.url", "database.user", "app.port");
```

#### 环境变量解析

```yaml
# 配置文件中的占位符
database:
  password: ${DB_PASSWORD}           # 必需的环境变量
  host: ${DB_HOST:localhost}         # 带默认值
  port: ${DB_PORT:3306}              # 数字类型自动转换
```

```java
// 代码中也可以直接读取环境变量
String dbHost = config.get("database.host");  // 自动解析 ${DB_HOST:localhost}
```

## Data Models

### 核心数据结构

所有数据使用Java标准类型，不定义额外的POJO：

- **请求/响应数据**: `Map<String, Object>`, `List<Object>`
- **配置**: `Map<String, Object>` 或 `Properties`
- **数据库结果**: `List<Map<String, Object>>`
- **缓存值**: `Object` (自动JSON序列化)

### UploadedFile

```java
public class UploadedFile {
    public String name;
    public String contentType;
    public long size;
    public byte[] content;
    
    public void saveTo(String path);
}
```

### Route (内部使用)

```java
class Route {
    String method;
    String pattern;
    Handler handler;
    List<Middleware> middlewares;
    List<String> paramNames; // 从pattern提取的参数名
}
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: JSON Round-Trip Consistency

*For any* valid Java object (Map, List, or primitive types), serializing to JSON and then parsing back should produce an equivalent object.

**Validates: Requirements 4.4, 5.1, 5.2, 5.4**

### Property 2: Middleware Execution Order (Onion Model)

*For any* sequence of N middlewares registered in order [M1, M2, ..., Mn], when a request is processed:
- The "before next" code executes in order: M1 → M2 → ... → Mn
- The "after next" code executes in reverse order: Mn → ... → M2 → M1

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 3: Middleware Chain Termination

*For any* middleware chain where middleware Mi does not call next(), middlewares Mi+1 through Mn should not execute.

**Validates: Requirements 2.4**

### Property 4: Route Matching and Method Dispatch

*For any* set of registered routes and any incoming request, the framework should invoke exactly the handler whose path pattern and HTTP method match the request.

**Validates: Requirements 3.1, 3.3**

### Property 5: Path Parameter Extraction

*For any* route pattern with parameters (e.g., `/users/:id/posts/:postId`) and a matching request path, all parameter values should be correctly extracted and accessible by name in the context.

**Validates: Requirements 3.2, 4.5**

### Property 6: Route Group Prefix Application

*For any* route group with prefix P and routes with paths [R1, R2, ...], the effective paths should be [P+R1, P+R2, ...].

**Validates: Requirements 3.5**

### Property 7: Unmatched Route Returns 404

*For any* request path that does not match any registered route, the response status code should be 404.

**Validates: Requirements 3.4**

### Property 8: Context Contains Request Information

*For any* HTTP request with method M, path P, headers H, and query parameters Q, the created Context should have method=M, path=P, headers containing all of H, and queryParams containing all of Q.

**Validates: Requirements 4.1**

### Property 9: Response Configuration Consistency

*For any* sequence of response configuration calls (status, header, body), the final response should reflect all configured values.

**Validates: Requirements 4.3**

### Property 10: Invalid JSON Throws ParseException

*For any* string that is not valid JSON, calling bodyJson() should throw a JsonParseException with error details.

**Validates: Requirements 5.3**

### Property 11: Plugin Registration and Access

*For any* plugin registered with name N and configuration C, accessing ctx.plugin(N) should return the initialized plugin instance that received configuration C.

**Validates: Requirements 6.1, 6.2, 6.3**

### Property 12: Plugin Initialization Order

*For any* sequence of plugins [P1, P2, ..., Pn] registered in order, initialization should occur in the same order: P1 → P2 → ... → Pn.

**Validates: Requirements 6.5**

### Property 13: SQL Parameter Binding Safety

*For any* parameterized SQL query with parameters containing special characters (quotes, semicolons, etc.), the parameters should be safely bound without allowing SQL injection.

**Validates: Requirements 7.4**

### Property 14: Cache Round-Trip Consistency

*For any* key-value pair stored in cache, retrieving by the same key should return an equivalent value (before TTL expiration).

**Validates: Requirements 8.2, 8.3**

### Property 15: Cache Deletion Removes Entry

*For any* cached entry with key K, after deletion, get(K) should return null.

**Validates: Requirements 8.4**

### Property 16: WebSocket Message Round-Trip

*For any* message sent through a WebSocket connection, the receiving end should receive the exact same message content.

**Validates: Requirements 9.2, 9.3**

### Property 17: Error Response in Dev Mode Contains Details

*For any* unhandled exception E in development mode, the response body should contain the exception message and stack trace information.

**Validates: Requirements 11.1**

### Property 18: Error Response in Prod Mode Hides Details

*For any* unhandled exception in production mode, the response body should not contain exception message, class name, or stack trace.

**Validates: Requirements 11.2**

### Property 19: Custom Error Handler Invocation

*For any* registered custom error handler and any unhandled exception, the custom handler should be invoked with the exception and context.

**Validates: Requirements 11.3**

### Property 20: Static File Serving

*For any* file F in the configured static directory at path P, a request to the static URL prefix + P should return the file content.

**Validates: Requirements 12.1**

### Property 21: Content-Type Mapping by Extension

*For any* static file with extension E, the response Content-Type header should match the MIME type for that extension.

**Validates: Requirements 12.2**

### Property 22: Static File 404 for Missing Files

*For any* request to a static file path where no file exists, the response status should be 404.

**Validates: Requirements 12.3**

### Property 23: Body Parsing by Content-Type

*For any* request with Content-Type header CT and body B:
- If CT is application/json, bodyJson() should return parsed JSON
- If CT is application/x-www-form-urlencoded, bodyForm() should return parsed form data
- If CT is multipart/form-data, bodyFiles() should return uploaded files

**Validates: Requirements 13.1, 13.2, 13.3**

### Property 24: Body Parsing Error Handling

*For any* request body that cannot be parsed according to its Content-Type, the parsing method should throw a clear exception.

**Validates: Requirements 13.4**

### Property 25: Middleware Exception Propagation

*For any* middleware that throws an exception E, the exception should propagate to the error handling middleware with the original exception information preserved.

**Validates: Requirements 2.5**

### Property 26: Configuration Value Type Safety

*For any* configuration key K with value V, calling the appropriate typed getter (getString, getInt, getBool, etc.) should return V converted to the correct type, or throw a clear exception if conversion fails.

**Validates: Requirements 14.3**

### Property 27: Configuration Merge Precedence

*For any* configuration key K defined in multiple sources (base file, profile file, environment variable), the value from the higher precedence source should override lower precedence sources.

**Validates: Requirements 14.2**

### Property 28: Configuration Placeholder Resolution

*For any* configuration value containing placeholder ${VAR} or ${VAR:default}, the placeholder should be resolved from environment variables, using the default value if the variable is not set.

**Validates: Requirements 14.7**

### Property 29: Configuration Sub-namespace Isolation

*For any* plugin namespace prefix P, calling config.sub(P) should return a Config containing only keys under that prefix, with the prefix stripped from key names.

**Validates: Requirements 14.6**

### Property 30: Required Configuration Validation

*For any* required configuration key K that is missing, calling require(K) should throw a ConfigException with the missing key name.

**Validates: Requirements 14.4, 14.8**

## Error Handling

### 错误处理策略

1. **框架异常层次**

```java
public class LiteJavaException extends RuntimeException {
    public int statusCode = 500;
    public Map<String, Object> details;
}

public class RouteNotFoundException extends LiteJavaException {
    { statusCode = 404; }
}

public class JsonParseException extends LiteJavaException {
    { statusCode = 400; }
}

public class PluginException extends LiteJavaException { }

public class DatabaseException extends LiteJavaException { }

public class CacheException extends LiteJavaException { }
```

2. **默认错误处理中间件**

```java
// 开发模式：返回详细错误信息
// 生产模式：返回通用错误信息，详细信息只记录日志
app.use((ctx, next) -> {
    try {
        next.run();
    } catch (LiteJavaException e) {
        ctx.status(e.statusCode);
        if (app.devMode) {
            ctx.json(Map.of(
                "error", e.getMessage(),
                "stack", getStackTrace(e),
                "details", e.details
            ));
        } else {
            ctx.json(Map.of("error", "Internal Server Error"));
            log.error("Request failed", e);
        }
    }
});
```

3. **自定义错误处理**

```java
app.onError((ctx, error) -> {
    // 自定义错误处理逻辑
    ctx.status(500).json(Map.of("message", "Something went wrong"));
});
```

## Testing Strategy

### 测试框架选择

- **单元测试**: JUnit 5
- **属性测试**: jqwik (Java Property-Based Testing)
- **HTTP测试**: 内置测试客户端

### 单元测试

单元测试覆盖具体示例和边界情况：

```java
@Test
void shouldParsePathParameters() {
    Route route = new Route("GET", "/users/:id/posts/:postId");
    Map<String, String> params = route.extractParams("/users/123/posts/456");
    assertEquals("123", params.get("id"));
    assertEquals("456", params.get("postId"));
}

@Test
void shouldReturn404ForUnmatchedRoute() {
    App app = new App();
    app.get("/users", ctx -> ctx.text("users"));
    
    TestResponse response = app.test().get("/posts");
    assertEquals(404, response.status);
}
```

### 属性测试

属性测试验证普遍性质，使用jqwik框架：

```java
// **Feature: lite-java-framework, Property 1: JSON Round-Trip Consistency**
@Property(tries = 100)
void jsonRoundTrip(@ForAll("validJsonObjects") Map<String, Object> original) {
    String json = Json.stringify(original);
    Map<String, Object> parsed = Json.parse(json);
    assertEquals(original, parsed);
}

// **Feature: lite-java-framework, Property 2: Middleware Execution Order**
@Property(tries = 100)
void middlewareExecutionOrder(@ForAll @IntRange(min = 1, max = 10) int count) {
    List<Integer> executionOrder = new ArrayList<>();
    App app = new App();
    
    for (int i = 0; i < count; i++) {
        final int index = i;
        app.use((ctx, next) -> {
            executionOrder.add(index);  // before
            next.run();
            executionOrder.add(count + index);  // after
        });
    }
    
    app.get("/test", ctx -> ctx.text("ok"));
    app.test().get("/test");
    
    // Verify onion model: 0,1,2,...,n-1 (before) then n+n-1,n+n-2,...,n (after)
    for (int i = 0; i < count; i++) {
        assertEquals(i, executionOrder.get(i));
        assertEquals(count + (count - 1 - i), executionOrder.get(count + i));
    }
}

// **Feature: lite-java-framework, Property 5: Path Parameter Extraction**
@Property(tries = 100)
void pathParameterExtraction(
    @ForAll @Size(min = 1, max = 5) List<@AlphaChars @StringLength(min = 1, max = 10) String> paramNames,
    @ForAll @Size(min = 1, max = 5) List<@AlphaChars @StringLength(min = 1, max = 20) String> paramValues
) {
    Assume.that(paramNames.size() == paramValues.size());
    
    String pattern = "/" + paramNames.stream().map(n -> ":" + n).collect(joining("/"));
    String path = "/" + String.join("/", paramValues);
    
    Route route = new Route("GET", pattern);
    Map<String, String> extracted = route.extractParams(path);
    
    for (int i = 0; i < paramNames.size(); i++) {
        assertEquals(paramValues.get(i), extracted.get(paramNames.get(i)));
    }
}
```

### 测试工具

框架内置测试客户端，方便编写测试：

```java
public class TestClient {
    public TestResponse get(String path);
    public TestResponse post(String path, Object body);
    public TestResponse put(String path, Object body);
    public TestResponse delete(String path);
    public TestResponse request(String method, String path, Map<String, String> headers, Object body);
}

public class TestResponse {
    public int status;
    public Map<String, String> headers;
    public String body;
    public <T> T json();
}
```

### 测试配置

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.9.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>net.jqwik</groupId>
        <artifactId>jqwik</artifactId>
        <version>1.8.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```
