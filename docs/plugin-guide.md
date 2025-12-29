# 插件开发指南

## 插件类型

LiteJava 有两种插件类型：

| 类型 | 基类 | 用途 |
|------|------|------|
| 普通插件 | `Plugin` | 功能扩展（数据库、缓存、配置等） |
| 中间件插件 | `MiddlewarePlugin` | 请求拦截（认证、日志、限流等） |

## 普通插件

### 基本结构

```java
public class MyPlugin extends Plugin {
    // 配置字段（public，支持代码设置和配置文件覆盖）
    public String endpoint;
    public int timeout = 30;
    
    @Override
    public void config() {
        // 1. 加载配置（配置文件优先）
        endpoint = app.conf.getString("my", "endpoint", endpoint);
        timeout = app.conf.getInt("my", "timeout", timeout);
        
        // 2. 初始化逻辑
        initClient();
    }
    
    @Override
    public void uninstall() {
        // 清理资源
        closeClient();
    }
}
```

### 配置优先级

```
use() 后直接设置 > 配置文件 > 构造参数 > 字段默认值
```

```java
// 配置文件有 my.timeout=60
MyPlugin plugin = new MyPlugin();
plugin.timeout = 30;        // 构造后设置
app.use(plugin);            // config() 执行，读取配置文件 → timeout=60
plugin.timeout = 90;        // use() 后设置，最终 timeout=90
```

### 生命周期

```
new Plugin() → app.use(plugin) → config() → onStart() → ... → uninstall()
```

| 方法 | 调用时机 | 用途 |
|------|----------|------|
| `config()` | `app.use()` 时立即调用 | 读取配置、初始化资源 |
| `onStart()` | `app.run()` 时调用 | 启动后逻辑（打印日志等） |
| `uninstall()` | 应用关闭时调用 | 清理资源 |

### 访问其他插件

```java
@Override
public void config() {
    // 通过 app 访问其他插件
    String dbUrl = app.conf.getString("database", "url", "");
    app.log.info("MyPlugin initialized");
    
    // 访问 JSON 插件
    String json = app.json.stringify(data);
}
```

### 注册到 App

```java
@Override
public void config() {
    // 注册到 app，供其他地方使用
    app.plugins.put("myPlugin", this);
    // 或注册到特定字段
    app.my = this;
}
```

## 中间件插件

### 基本结构

```java
public class AuthPlugin extends MiddlewarePlugin {
    public String headerName = "Authorization";
    
    @Override
    public void config() {
        headerName = app.conf.getString("auth", "header", headerName);
    }
    
    @Override
    public void handle(Context ctx) {
        // 前置逻辑
        String token = ctx.header(headerName);
        if (token == null) {
            ctx.fail(401, -1, "请先登录");
            return;  // 不调用 next()，中断请求
        }
        
        // 解析 token，存入 state
        ctx.state.put("userId", JwtUtil.verify(token));
        
        // 调用下一个中间件/handler
        ctx.next();
        
        // 后置逻辑（可选）
        // 此时响应已生成，可以做日志记录等
    }
}
```

### 洋葱模型

```
请求 → Auth → Log → Router → Handler
                              ↓
响应 ← Auth ← Log ← Router ←──┘
```

```java
public class LogPlugin extends MiddlewarePlugin {
    @Override
    public void handle(Context ctx) {
        long start = System.currentTimeMillis();
        
        ctx.next();  // 执行后续中间件和 handler
        
        // next() 返回后，响应已生成
        long cost = System.currentTimeMillis() - start;
        app.log.info("{} {} {}ms", ctx.method, ctx.path, cost);
    }
}
```

### 路径匹配

```java
// 全局中间件
app.use(new LogPlugin());

// 路径匹配
app.use("/api/*", new AuthPlugin());      // /api/ 开头的请求
app.use("/admin/*", new AdminAuthPlugin()); // /admin/ 开头的请求
```

## 完整示例：Redis 缓存插件

```java
public class RedisPlugin extends Plugin {
    // 配置字段
    public String host = "localhost";
    public int port = 6379;
    public String password;
    public int database = 0;
    public int timeout = 3000;
    
    // 内部状态
    public JedisPool pool;
    
    @Override
    public void config() {
        // 加载配置
        host = app.conf.getString("redis", "host", host);
        port = app.conf.getInt("redis", "port", port);
        password = app.conf.getString("redis", "password", password);
        database = app.conf.getInt("redis", "database", database);
        timeout = app.conf.getInt("redis", "timeout", timeout);
        
        // 初始化连接池
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(50);
        config.setMaxIdle(10);
        
        pool = new JedisPool(config, host, port, timeout, password, database);
        
        // 注册到 app
        app.redis = this;
        app.log.info("RedisPlugin: Connected to {}:{}", host, port);
    }
    
    @Override
    public void uninstall() {
        if (pool != null) {
            pool.close();
        }
    }
    
    // 便捷方法
    public String get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
        }
    }
    
    public void set(String key, String value, int seconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, seconds, value);
        }
    }
    
    public void del(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }
}
```

配置文件：

```yaml
redis:
  host: localhost
  port: 6379
  password: 
  database: 0
```

使用：

```java
app.use(new RedisPlugin());

// 在 handler 中
app.redis.set("user:1", json, 3600);
String cached = app.redis.get("user:1");
```

## 完整示例：限流中间件

```java
public class RateLimitPlugin extends MiddlewarePlugin {
    public int limit = 100;        // 每窗口最大请求数
    public int windowSeconds = 60; // 窗口大小（秒）
    
    Map<String, int[]> counters = new ConcurrentHashMap<>();
    
    @Override
    public void config() {
        limit = app.conf.getInt("rateLimit", "limit", limit);
        windowSeconds = app.conf.getInt("rateLimit", "window", windowSeconds);
    }
    
    @Override
    public void handle(Context ctx) {
        String key = ctx.clientIP();
        long now = System.currentTimeMillis();
        long window = now / (windowSeconds * 1000);
        
        int[] counter = counters.computeIfAbsent(key, k -> new int[]{0, (int) window});
        
        synchronized (counter) {
            if (counter[1] != window) {
                counter[0] = 0;
                counter[1] = (int) window;
            }
            counter[0]++;
            
            if (counter[0] > limit) {
                ctx.status(429).fail("请求过于频繁，请稍后再试");
                return;
            }
        }
        
        ctx.next();
    }
}
```

## 最佳实践

1. **配置字段用 public** - 方便代码直接设置
2. **config() 开头加载配置** - 集中管理，清晰明了
3. **字段名与配置 key 一致** - 减少心智负担
4. **uninstall() 清理资源** - 避免内存泄漏
5. **提供便捷方法** - 封装常用操作
6. **打印启动日志** - 方便排查问题
