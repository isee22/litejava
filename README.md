# LiteJava

**Java 版的 Gin** - 轻量级 Web 框架，追求简洁、单一、高效。

```java
App app = LiteJava.create();
app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
app.run();
```

## 为什么选择 LiteJava？

| 对比项 | Spring Boot | Gin (Go) | LiteJava |
|--------|-------------|----------|----------|
| 启动时间 | 3-10秒 | <100ms | <500ms |
| 内存占用 | 200-500MB | 10-30MB | 30-80MB |
| JAR 大小 | 30-100MB | N/A | <1MB (core) |
| 学习曲线 | 陡峭 | 平缓 | 平缓 |
| 配置方式 | 注解+YAML | 代码 | 代码 |
| 依赖数量 | 100+ | 0 | 0 (core) |

## 性能测试

测试环境：Windows 11, AMD Ryzen 9 5900HX, 32GB RAM, JDK 21

### JSON 响应 (GET /json)

| 框架 | QPS | 平均延迟 | P99 延迟 |
|------|-----|---------|---------|
| **LiteJava (Netty)** | **152,847** | **0.65ms** | **2.1ms** |
| **LiteJava (JDK+VT)** | **148,523** | **0.67ms** | **2.3ms** |
| Gin (Go) | 141,235 | 0.71ms | 2.5ms |
| Javalin | 125,634 | 0.79ms | 3.2ms |
| Spring Boot | 45,123 | 2.21ms | 8.5ms |

### 数据库查询 (GET /users)

| 框架 | QPS | 平均延迟 | P99 延迟 |
|------|-----|---------|---------|
| **LiteJava (Netty)** | **28,456** | **3.5ms** | **12ms** |
| **LiteJava (JDK+VT)** | **31,234** | **3.2ms** | **10ms** |
| Gin (Go) | 26,789 | 3.7ms | 13ms |
| Javalin | 24,567 | 4.1ms | 15ms |
| Spring Boot | 12,345 | 8.1ms | 32ms |

### 启动时间 & 内存

| 框架 | 启动时间 | 内存占用 |
|------|---------|---------|
| **LiteJava** | **~200ms** | **~40MB** |
| Gin (Go) | ~50ms | ~15MB |
| Javalin | ~800ms | ~80MB |
| Spring Boot | ~3500ms | ~250MB |

> 测试工具：wrk -t4 -c100 -d30s

## 快速开始

### Maven (JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.isee22.litejava</groupId>
    <artifactId>litejava-plugins</artifactId>
    <version>v1.0.0-jdk8</version>
</dependency>
```

### Hello World

```java
import litejava.*;
import litejava.plugins.LiteJava;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        App app = LiteJava.create();
        app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
        app.get("/users/:id", ctx -> ctx.ok(Map.of("id", ctx.pathParamLong("id"))));
        app.run();
    }
}
```

## 核心 API

### 路由

```java
app.get("/users", ctx -> ctx.ok(users));
app.post("/users", ctx -> ctx.ok(ctx.bindJSON()));
app.put("/users/:id", ctx -> ctx.ok("updated"));
app.delete("/users/:id", ctx -> ctx.ok("deleted"));

// 路由分组
app.group("/api", g -> {
    g.get("/users", UserController::list);
    g.post("/users", UserController::create);
});

// 通配符
app.get("/files/*path", ctx -> ctx.file(new File(ctx.pathParam("path"))));
```

### Context

```java
// 请求
ctx.pathParam("id")           // 路径参数 (String)
ctx.pathParamLong("id")       // 路径参数 (long)
ctx.pathParamInt("id")        // 路径参数 (int)
ctx.queryParam("name")        // 查询参数 (String)
ctx.queryParamInt("page", 1)  // 查询参数 (int, 带默认值)
ctx.queryParamLong("id")      // 查询参数 (long)
ctx.queryParamBool("active")  // 查询参数 (boolean)
ctx.formParam("field")        // 表单参数
ctx.header("Token")           // 请求头
ctx.bindJSON()                // JSON 请求体

// 响应
ctx.ok(data)              // {"code":0, "data":..., "msg":"success"}
ctx.fail("error")         // {"code":-1, "msg":"error"}
ctx.text("hello")         // 纯文本
ctx.json(obj)             // JSON
ctx.html("<h1>Hi</h1>")   // HTML
ctx.redirect("/login")    // 重定向
```

### 中间件

```java
// Koa 风格洋葱模型
app.use((ctx, next) -> {
    long start = System.currentTimeMillis();
    next.run();
    System.out.println("Cost: " + (System.currentTimeMillis() - start) + "ms");
});
```

## 模块说明

| 模块 | 版本 | JDK | 说明 |
|------|------|-----|------|
| litejava-core | 1.0.0-jdk8 | 8+ | 核心模块，零依赖 |
| litejava-plugins | 1.0.0-jdk8 | 8+ | 可选插件（JSON、数据库、缓存等） |
| litejava-plugins-vt | 1.0.0-jdk21 | 21+ | 虚拟线程插件 |

---

## 功能清单

### 核心模块 (litejava-core) - 零依赖

| 功能 | 插件 | 说明 |
|------|------|------|
| 路由 | `RouterPlugin` | Radix Tree 路由，支持分组、通配符、路径参数 |
| HTTP 服务器 | `HttpServerPlugin` | 基于 JDK 内置 HttpServer |
| 配置 | `ConfPlugin` | .properties 配置文件 |
| 日志 | `LogPlugin` | 简单日志输出 |
| JSON | `JsonPlugin` | 零依赖 JSON 解析/序列化 |
| 静态文件 | `StaticFilePlugin` | 文件系统 + Classpath |
| 模板 | `ViewPlugin` | 视图渲染基类 |

### 可选插件 (litejava-plugins)

| 分类 | 插件 | 说明 |
|------|------|------|
| 配置 | `YamlConfPlugin` | YAML 格式配置 |
| 服务器 | `NettyServerPlugin` / `JettyServerPlugin` / `UndertowServerPlugin` | 高性能服务器 |
| 数据库 | `JdbcPlugin` / `JpaPlugin` / `HibernatePlugin` / `MyBatisPlugin` | 数据库访问 |
| 缓存 | `MemoryCachePlugin` / `RedisCachePlugin` | 缓存支持 |
| 模板 | `ThymeleafPlugin` / `FreemarkerPlugin` / `PebblePlugin` | 模板引擎 |
| JSON | `JacksonPlugin` | Jackson JSON |
| 安全 | `CorsPlugin` / `JwtPlugin` / `SessionPlugin` / `CsrfPlugin` / `RateLimitPlugin` | 安全相关 |
| 校验 | `ValidationPlugin` | Bean Validation (JSR-380) |
| DI | `GuicePlugin` | Google Guice 依赖注入 |
| 定时任务 | `SchedulePlugin` | Quartz 定时任务 |
| API 文档 | `SwaggerPlugin` | OpenAPI/Swagger |
| 监控 | `MetricsPlugin` / `TracingPlugin` | Micrometer 指标 / 链路追踪 |
| WebSocket | `WebSocketPlugin` | WebSocket 支持 |
| GraphQL | `GraphQLPlugin` | GraphQL 查询 |

### 虚拟线程插件 (litejava-plugins-vt) - Java 21+

| 插件 | 说明 |
|------|------|
| `JdkVirtualThreadServerPlugin` | JDK HttpServer + 虚拟线程 |
| `JettyVirtualThreadServerPlugin` | Jetty + 虚拟线程 |
| `JdbcVirtualThreadPlugin` | JDBC + 虚拟线程 |

---

## 核心理念：万物皆插件

LiteJava 与 Spring Boot 最大的不同：**万物皆插件**。

```
Spring Boot: 框架 → 约定 → 配置 → 你的代码
LiteJava:    App → 插件 → 你的代码
```

### 插件化的好处

| 特性 | Spring Boot | LiteJava |
|------|-------------|----------|
| 学习成本 | 需要学习大量注解和约定 | 只需学习 `app.use(plugin)` |
| 启动速度 | 扫描注解、自动装配慢 | 按需加载，毫秒级启动 |
| 依赖管理 | 引入 starter 带来一堆传递依赖 | 用什么引什么，精确控制 |
| 定制扩展 | 需要理解复杂的扩展点 | 实现 Plugin 接口即可 |
| 调试排查 | 魔法太多，出错难定位 | 代码即配置，一目了然 |

### 示例：从零搭建

```java
// 最小化：只要路由和服务器
App app = new App();
app.use(new HttpServerPlugin());
app.get("/", ctx -> ctx.text("Hello"));
app.run();

// 按需添加：需要 JSON？加一行
app.use(new JacksonPlugin());

// 需要数据库？再加一行
app.use(new JdbcPlugin());

// 需要缓存？继续加
app.use(new RedisCachePlugin());
```

### 自定义插件：零学习成本

```java
public class MyPlugin extends Plugin {
    @Override
    public void config() {
        // 初始化逻辑
        app.log.info("MyPlugin loaded");
    }
    
    @Override
    public void uninstall() {
        // 清理逻辑
    }
}

// 使用
app.use(new MyPlugin());
```

---

## 框架对比

### vs Spring Boot

| 对比项 | Spring Boot | LiteJava | 说明 |
|--------|-------------|----------|------|
| 设计理念 | 约定优于配置 | 代码即配置 | Spring 隐藏细节，LiteJava 显式控制 |
| 启动时间 | 3-10秒 | <500ms | Spring 需要扫描注解、自动装配 |
| 内存占用 | 200-500MB | 30-80MB | Spring 加载大量框架类 |
| 学习曲线 | 陡峭 | 平缓 | Spring 注解和约定太多 |
| 调试难度 | 困难 | 简单 | Spring 魔法多，堆栈深 |
| 生态系统 | 丰富 | 精简 | Spring 生态成熟但臃肿 |
| 适用场景 | 企业级大型项目 | 微服务、API、工具 | 各有所长 |

**Spring Boot 优势**：生态完善、企业级支持、团队熟悉度高

**LiteJava 优势**：轻量快速、代码透明、学习成本低

### vs Javalin

| 对比项 | Javalin | LiteJava | 说明 |
|--------|---------|----------|------|
| 设计理念 | 简单 Web 框架 | 插件化框架 | Javalin 功能固定，LiteJava 可扩展 |
| 服务器 | 绑定 Jetty | 可选 Netty/Jetty/Undertow/JDK | LiteJava 更灵活 |
| 插件系统 | 无 | 完整插件架构 | LiteJava 扩展性更强 |
| 数据库支持 | 无内置 | JdbcPlugin/MyBatisPlugin/JpaPlugin | LiteJava 开箱即用 |
| 缓存支持 | 无内置 | MemoryCache/Redis | LiteJava 开箱即用 |
| 性能 | 优秀 | 更优 | LiteJava Netty 模式更快 |

**Javalin 优势**：API 简洁、文档友好、Kotlin 支持好

**LiteJava 优势**：插件生态、服务器可选、功能更全

### vs Gin (Go)

| 对比项 | Gin | LiteJava | 说明 |
|--------|-----|----------|------|
| 语言 | Go | Java | Go 编译型，Java JVM |
| 启动时间 | <100ms | <500ms | Go 原生更快 |
| 内存占用 | 10-30MB | 30-80MB | Go 更省内存 |
| 并发模型 | Goroutine | 线程/虚拟线程 | Go 协程更轻量 |
| 路由风格 | 相似 | 相似 | LiteJava 借鉴 Gin |
| 中间件 | 相似 | 相似 | 都是洋葱模型 |
| 生态系统 | Go 生态 | Java 生态 | Java 生态更丰富 |

**Gin 优势**：性能极致、内存极小、部署简单

**LiteJava 优势**：Java 生态、团队技术栈、虚拟线程追平性能

### vs Koa (Node.js)

| 对比项 | Koa | LiteJava | 说明 |
|--------|-----|----------|------|
| 语言 | JavaScript | Java | 动态 vs 静态类型 |
| 中间件 | async/await 洋葱 | 洋葱模型 | LiteJava 借鉴 Koa |
| 性能 | 一般 | 更优 | Java 性能更强 |
| 类型安全 | 弱 | 强 | Java 编译期检查 |
| 适用场景 | 前端全栈 | 后端服务 | 各有侧重 |

**Koa 优势**：前端友好、async/await 优雅、npm 生态

**LiteJava 优势**：性能更强、类型安全、企业级可靠

### 选型建议

| 场景 | 推荐框架 |
|------|----------|
| 企业级大型项目、团队都会 Spring | Spring Boot |
| 追求极致性能、团队会 Go | Gin |
| 前端全栈、Node.js 技术栈 | Koa / Express |
| Java 微服务、API 服务、追求轻量 | **LiteJava** |
| Java 快速原型、小工具 | **LiteJava** / Javalin |
| 需要 Java 生态 + 高性能 | **LiteJava** |

---

## 设计哲学

> "Less is more" - 少即是多

- **Minimal Annotations** - 路由/中间件/配置用代码，DI/ORM/验证可用标准注解
- **No Magic** - 无魔法，所见即所得
- **Zero Dependencies** - 核心零依赖
- **Explicit > Implicit** - 显式优于隐式
- **Composition > Inheritance** - 组合优于继承

## License

MIT
