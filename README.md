# LiteJava

<p align="center">
  <strong>🚀 极简 Java Web 框架</strong><br>
  性能比肩 Go，轻量高效，插件化扩展，无缝融合 Java 生态
</p>

<p align="center">
  <a href="#快速开始">快速开始</a> •
  <a href="#为什么选择-litejava">为什么选择</a> •
  <a href="#核心特性">核心特性</a> •
  <a href="#插件生态">插件生态</a> •
  <a href="#性能测试">性能测试</a>
</p>

---

## 30 秒上手

```java
import litejava.*;
import litejava.plugins.LiteJava;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        app.get("/", ctx -> ctx.json(Map.of("message", "Hello, LiteJava!")));
        
        app.get("/users/:id", ctx -> {
            long id = ctx.pathParamLong("id");
            ctx.json(Map.of("id", id, "name", "User " + id));
        });
        
        app.run();  // 启动！访问 http://localhost:8080
    }
}
```

**就这么简单。** 没有 XML，没有注解地狱，没有 30 秒的启动等待。

---

## 为什么选择 LiteJava？

### 🎯 如果你厌倦了...

- **Spring Boot 的臃肿** - 启动 10 秒，内存 500MB，一个 Hello World 引入 100+ 依赖
- **注解的泛滥** - `@RestController` `@RequestMapping` `@Autowired` `@Service` `@Component`...
- **魔法般的自动装配** - 出了问题不知道哪里错，堆栈 50 层看不懂
- **配置的复杂** - application.yml 写了 200 行还没配完

### ✨ LiteJava 给你...

| 痛点 | Spring Boot | LiteJava |
|------|-------------|----------|
| 启动时间 | 3-10 秒 | **< 500ms** |
| 内存占用 | 200-500 MB | **30-80 MB** |
| JAR 大小 | 30-100 MB | **< 1 MB** (core) |
| 依赖数量 | 100+ | **0** (core) |
| 学习曲线 | 陡峭（注解+约定太多） | **平缓**（代码即配置） |
| 调试难度 | 困难（魔法太多） | **简单**（所见即所得） |

---

## 核心特性

### 1️⃣ Gin-style 路由

```java
// 基础路由
app.get("/users", ctx -> ctx.json(userService.list()));
app.post("/users", ctx -> ctx.json(userService.create(ctx.bindJSON())));
app.put("/users/:id", ctx -> ctx.json(userService.update(ctx.pathParamLong("id"), ctx.bindJSON())));
app.delete("/users/:id", ctx -> ctx.ok(userService.delete(ctx.pathParamLong("id"))));

// 路由分组 - 告别重复前缀
app.group("/api/v1", api -> {
    api.get("/books", BookController::list);       // GET /api/v1/books
    api.post("/books", BookController::create);    // POST /api/v1/books
    api.get("/books/:id", BookController::get);    // GET /api/v1/books/:id
});

// 嵌套分组 + 分组级中间件
app.group("/admin", admin -> {
    admin.use(new AuthPlugin(token -> jwtPlugin.verify(token)));  // 只对 /admin/* 生效
    admin.group("/users", users -> {
        users.get("/", UserController::list);
        users.delete("/:id", UserController::delete);
    });
});

// 通配符路由
app.get("/files/*filepath", ctx -> ctx.file(new File(uploadDir, ctx.pathParam("filepath"))));
```

### 2️⃣ Koa-style 洋葱中间件

```java
// 请求日志中间件
app.use((ctx, next) -> {
    long start = System.currentTimeMillis();
    System.out.println("--> " + ctx.method + " " + ctx.path);
    
    next.run();  // 执行后续中间件和 handler
    
    long cost = System.currentTimeMillis() - start;
    System.out.println("<-- " + ctx.status + " " + cost + "ms");
});


// 认证插件 - 使用内置 AuthPlugin
app.use(new AuthPlugin(token -> {
    // 自定义验证逻辑，返回用户信息 Map 或 null
    return jwtPlugin.verify(token);
}).whitelist("/", "/login").whitelistPrefix("/static"));

// 在 Handler 中获取认证信息
app.get("/api/me", ctx -> {
    Map<String, Object> user = (Map) ctx.state.get("auth");
    ctx.ok(user);
});
```

### 3️⃣ 简洁的 Context API

```java
app.post("/users", ctx -> {
    // 获取参数
    String name = ctx.queryParam("name");              // 查询参数
    int page = ctx.queryParamInt("page", 1);           // 带默认值
    long id = ctx.pathParamLong("id");                 // 路径参数
    String token = ctx.header("Authorization");        // 请求头
    User user = ctx.bindJSON(User.class);              // JSON 请求体
    
    // 响应
    ctx.ok(data);                    // {"code":0, "data":..., "msg":"success"}
    ctx.fail("error message");       // {"code":-1, "msg":"error message"}
    ctx.json(obj);                   // 原始 JSON
    ctx.text("hello");               // 纯文本
    ctx.html("<h1>Hi</h1>");         // HTML
    ctx.redirect("/login");          // 重定向
    ctx.file(new File("doc.pdf"));   // 文件下载
    ctx.render("user.html", model);  // 模板渲染
});
```

### 4️⃣ 万物皆插件

**不强制任何功能，自由装配你需要的插件：**

```java
// 最小化启动 - 只要路由和服务器
App app = new App();
app.use(new HttpServerPlugin());
app.get("/", ctx -> ctx.text("Hello"));
app.run();

// 按需添加功能
app.use(new JacksonPlugin());        // 需要 JSON？
app.use(new JdbcPlugin());           // 需要数据库？
app.use(new RedisCachePlugin());     // 需要缓存？
app.use(new ThymeleafPlugin());      // 需要模板？
app.use(new SwaggerPlugin());        // 需要 API 文档？

// 或者一键启动（预装常用插件）
App app = LiteJava.create();  // 包含 Jackson + MemoryCache + HttpServer
```

**插件可随时替换，对业务代码零影响：**

```java
// 开发环境：用内置 HttpServer
app.use(new HttpServerPlugin());

// 生产环境：换成 Netty 高性能服务器
app.use(new NettyServerPlugin());

// 或者用虚拟线程版本 (Java 21+)
app.use(new JdkVirtualThreadServerPlugin());
```

**有趣的工具插件，开发调试更轻松：**

```java
// DebugPlugin - 启动时打印应用结构
app.use(new DebugPlugin());

// 输出：
// ╔══════════════════════════════════════════════════════════════╗
// ║                      My Application                          ║
// ╠══════════════════════════════════════════════════════════════╣
// ║ Plugins (6):                                                 ║
// ║   1. Slf4jLogPlugin                                          ║
// ║   2. JacksonPlugin                                           ║
// ║   3. HttpServerPlugin                                        ║
// ╠══════════════════════════════════════════════════════════════╣
// ║ Middleware Chain (2):                                        ║
// ║   Request → [RecoveryPlugin] → [CorsPlugin] → Handler        ║
// ╚══════════════════════════════════════════════════════════════╝
```

**配置也可以装在插件里：**

```java
// 默认：.properties 配置
app.use(new ConfPlugin());           // app.conf.get("key")

// 想用 YAML？换个插件
app.use(new YamlConfPlugin());       // 同样的 API，不同的配置格式

// 想从环境变量读取？自己写个插件
app.use(new EnvConfPlugin());        // 插件机制让扩展变得简单
```

---

## 真实项目示例

### RESTful API 服务

```java
public class BookApp {
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 数据库
        app.use(new JdbcPlugin("db"));
        JdbcPlugin jdbc = app.getPlugin(JdbcPlugin.class);
        
        // 图书 CRUD
        app.group("/api/books", books -> {
            books.get("/", ctx -> {
                int page = ctx.queryParamInt("page", 1);
                int size = ctx.queryParamInt("size", 20);
                List<Map<String, Object>> list = jdbc.jdbcTemplate.queryForList(
                    "SELECT * FROM books LIMIT ? OFFSET ?", size, (page - 1) * size
                );
                ctx.ok(Map.of("data", list, "page", page, "size", size));
            });
            
            books.get("/:id", ctx -> {
                Map<String, Object> book = jdbc.jdbcTemplate.queryForMap(
                    "SELECT * FROM books WHERE id = ?", ctx.pathParamLong("id")
                );
                ctx.ok(book);
            });
            
            books.post("/", ctx -> {
                Map<String, Object> data = ctx.bindJSON();
                jdbc.jdbcTemplate.update(
                    "INSERT INTO books (title, author) VALUES (?, ?)",
                    data.get("title"), data.get("author")
                );
                ctx.ok("created");
            });
            
            books.delete("/:id", ctx -> {
                jdbc.jdbcTemplate.update("DELETE FROM books WHERE id = ?", ctx.pathParamLong("id"));
                ctx.ok("deleted");
            });
        });
        
        app.run();
    }
}
```

### 带认证的微服务

```java
public class UserService {
    public static void main(String[] args) {
        App app = LiteJava.create();
        app.use(new JwtPlugin("your-secret-key"));
        app.use(new ValidationPlugin());
        app.use(new SwaggerPlugin().scanPackages("com.example.controller"));
        
        // 公开接口
        app.post("/auth/login", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            String token = JwtPlugin.instance.sign(Map.of("userId", 123));
            ctx.ok(Map.of("token", token));
        });
        
        // 需要认证的接口
        app.group("/api", api -> {
            api.use(new AuthPlugin(token -> JwtPlugin.instance.verify(token)));
            
            api.get("/me", ctx -> {
                Map<String, Object> user = (Map) ctx.state.get("auth");
                ctx.ok(user);
            });
            
            api.put("/me", ctx -> {
                // 参数校验
                Map<String, Object> body = ctx.bindJSON();
                ValidationPlugin.instance.validate(body, Map.of(
                    "name", "required|min:2|max:50",
                    "email", "required|email"
                ));
                ctx.ok("updated");
            });
        });
        
        app.run(8080);
    }
}
```

---

## 插件生态

### 核心模块 (litejava-core) - 零依赖

| 插件 | 说明 |
|------|------|
| `RouterPlugin` | Radix Tree 路由，支持分组、通配符、路径参数 |
| `HttpServerPlugin` | 基于 JDK 内置 HttpServer |
| `ConfPlugin` | .properties 配置文件 |
| `LogPlugin` | 简单日志输出 |
| `JsonPlugin` | 零依赖 JSON 解析/序列化 |
| `StaticFilePlugin` | 静态文件服务 |
| `ViewPlugin` | 视图渲染基类 |

### 可选插件 (litejava-plugins)

| 分类 | 插件 | 说明 |
|------|------|------|
| **服务器** | `NettyServerPlugin` | Netty 高性能服务器 |
| | `JettyServerPlugin` | Jetty 服务器 |
| | `UndertowServerPlugin` | Undertow 服务器 |
| **数据库** | `JdbcPlugin` | JDBC 数据库访问 |
| | `JpaPlugin` | JPA ORM |
| | `MyBatisPlugin` | MyBatis 集成 |
| **缓存** | `MemoryCachePlugin` | 内存缓存 |
| | `RedisCachePlugin` | Redis 缓存 |
| **JSON** | `JacksonPlugin` | Jackson JSON |
| **模板** | `ThymeleafPlugin` | Thymeleaf 模板 |
| | `FreemarkerPlugin` | Freemarker 模板 |
| **安全** | `JwtPlugin` | JWT 认证 |
| | `SessionPlugin` | Session 管理 |
| | `CorsPlugin` | 跨域处理 |
| | `CsrfPlugin` | CSRF 防护 |
| | `RateLimitPlugin` | 限流 |
| **校验** | `ValidationPlugin` | Bean Validation (JSR-380) |
| **DI** | `GuicePlugin` | Google Guice 依赖注入 |
| **定时任务** | `SchedulePlugin` | Quartz 定时任务 |
| **API 文档** | `SwaggerPlugin` | OpenAPI/Swagger 文档 |
| **监控** | `MetricsPlugin` | Micrometer 指标 |
| | `TracingPlugin` | 链路追踪 |
| **其他** | `WebSocketPlugin` | WebSocket 支持 |
| | `GraphQLPlugin` | GraphQL 查询 |

### 虚拟线程插件 (litejava-plugins-vt) - Java 21+

| 插件 | 说明 |
|------|------|
| `JdkVirtualThreadServerPlugin` | JDK HttpServer + 虚拟线程 |
| `JettyVirtualThreadServerPlugin` | Jetty + 虚拟线程 |

---

## 性能测试

**🔥 性能比肩 Go 语言框架，部分场景甚至超越 Gin**

> 测试环境：Windows 11, AMD Ryzen 9 5900HX, 32GB RAM, JDK 21  
> 测试工具：wrk -t4 -c100 -d30s

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


---

## 快速开始

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<!-- 只需要核心功能（零依赖） -->
<dependency>
    <groupId>com.github.isee22.litejava</groupId>
    <artifactId>litejava-core</artifactId>
    <version>v1.0.0-jdk8</version>
</dependency>

<!-- 需要完整插件生态 -->
<dependency>
    <groupId>com.github.isee22.litejava</groupId>
    <artifactId>litejava-plugins</artifactId>
    <version>v1.0.0-jdk8</version>
</dependency>

<!-- Java 21+ 虚拟线程支持 -->
<dependency>
    <groupId>com.github.isee22.litejava</groupId>
    <artifactId>litejava-plugins-vt</artifactId>
    <version>v1.0.0-jdk21</version>
</dependency>
```

### 模块说明

| 模块 | JDK | 说明 |
|------|-----|------|
| litejava-core | 8+ | 核心模块，零依赖 |
| litejava-plugins | 8+ | 可选插件（JSON、数据库、缓存等） |
| litejava-plugins-vt | 21+ | 虚拟线程插件 |

---

## 自定义插件

创建自己的插件非常简单：

```java
public class MyPlugin extends Plugin {
    
    public static MyPlugin instance;
    
    @Override
    public void config() {
        instance = this;
        // 初始化逻辑
        app.log.info("MyPlugin loaded!");
        
        // 可以注册路由
        app.get("/my-plugin/status", ctx -> ctx.ok("running"));
        
        // 可以添加中间件
        app.use((ctx, next) -> {
            ctx.header("X-My-Plugin", "1.0");
            next.run();
        });
    }
    
    @Override
    public void uninstall() {
        // 清理逻辑
        instance = null;
    }
    
    // 插件提供的功能
    public void doSomething() {
        // ...
    }
}

// 使用
app.use(new MyPlugin());
MyPlugin.instance.doSomething();
```

---

## 框架对比

### 什么时候用 LiteJava？

✅ **适合场景：**
- 微服务、API 服务
- 追求轻量和快速启动
- 厌倦了 Spring Boot 的复杂
- 想要 Go/Gin 风格的 Java 开发体验
- 需要精确控制依赖
- 快速原型开发

❌ **不适合场景：**
- 团队只会 Spring，不想学新东西
- 需要 Spring 生态的特定功能
- 企业级大型单体应用

### vs Spring Boot - 深度对比

Spring Boot 确实解决了很多问题，但代价是什么？LiteJava 用更轻量的方式提供同样的能力：

| Spring 解决的问题 | Spring 的方式 | LiteJava 的方式 |
|------------------|--------------|----------------|
| **依赖注入 (DI)** | `@Autowired` + 容器扫描，启动慢 | `GuicePlugin` 可选集成，或直接构造函数传参 |
| **路由映射** | `@RequestMapping` 注解，分散在各类 | 代码集中定义 `app.get("/path", handler)` |
| **配置管理** | `@Value` + `@ConfigurationProperties` | `ConfPlugin` 读取，`app.conf.get("key")` |
| **数据库访问** | `@Repository` + Spring Data | `JdbcPlugin` / `MyBatisPlugin` 插件 |
| **事务管理** | `@Transactional` 注解 | `JdbcPlugin.transaction(conn -> {...})` |
| **缓存** | `@Cacheable` + 自动代理 | `RedisCachePlugin` 显式调用 |
| **参数校验** | `@Valid` + `@NotNull` | `ValidationPlugin` 可选，代码校验同样简洁 |
| **AOP 切面** | `@Aspect` + 动态代理 | 中间件 `app.use((ctx, next) -> {...})` |
| **定时任务** | `@Scheduled` | `SchedulePlugin` 插件 |
| **API 文档** | Springfox/SpringDoc 注解 | `SwaggerPlugin` 插件 |

**关键区别：**

- **Spring**：功能深度绑定框架，注解侵入业务代码，移除困难
- **LiteJava**：功能以插件形式提供，对框架核心零侵入，随时可插拔

```java
// Spring 方式 - 注解侵入业务代码
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired private UserService userService;
    @Autowired private CacheManager cache;
    
    @GetMapping("/{id}")
    @Cacheable("users")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}

// LiteJava 方式 - 代码即配置，无侵入
app.get("/users/:id", ctx -> {
    long id = ctx.pathParamLong("id");
    User user = cache.get("user:" + id, () -> userService.findById(id));
    ctx.ok(user);
});
```

**即使需要注解，LiteJava 也能通过插件无侵入地支持：**

```java
// 想用 JSR-330 依赖注入？加个插件
app.use(new GuicePlugin());

// 想用 Bean Validation？加个插件
app.use(new ValidationPlugin());

// 想用 JPA 注解？加个插件
app.use(new JpaPlugin());

// 不想用了？移除插件即可，业务代码无需改动
```

| | Spring Boot | LiteJava |
|--|-------------|----------|
| 理念 | 约定优于配置 | 代码即配置 |
| 优势 | 企业级支持、社区成熟 | 轻量快速、代码透明、插件灵活 |
| 劣势 | 臃肿、魔法多、启动慢、注解侵入 | 社区较新 |

### vs Javalin

| | Javalin | LiteJava |
|--|---------|----------|
| 理念 | 简单 Web 框架 | 插件化框架 |
| 优势 | API 简洁、文档友好 | 插件生态、服务器可选 |
| 劣势 | 功能固定、绑定 Jetty | 相对较新 |

### vs Gin (Go)

| | Gin | LiteJava |
|--|-----|----------|
| 语言 | Go | Java |
| 性能 | 极致 | **比肩 Go（见性能测试）** |
| 优势 | 内存极小 | 插件生态丰富、扩展性强、Java 库无缝集成 |
| 劣势 | 扩展能力有限、需要学 Go | 内存占用略高 |

---

## 设计哲学

> "Less is more" - 少即是多

- **代码即配置** - 路由、中间件、配置都用代码，不用注解
- **零魔法** - 所见即所得，无隐藏规则
- **零依赖** - 核心模块不依赖任何第三方库
- **显式优于隐式** - 明确胜过猜测
- **组合优于继承** - 插件组合而非类继承

### 注解策略

LiteJava 不是完全禁止注解，而是控制边界：

| 层级 | 策略 | 说明 |
|------|------|------|
| 路由/中间件 | ❌ 不用 | `app.get("/users", handler)` |
| 配置 | ❌ 不用 | 配置文件 + 代码读取 |
| DI | ✅ 可选 | `@Inject`, `@Singleton` (JSR-330) |
| ORM | ✅ 可选 | `@Entity`, `@Table` (JPA) |
| 校验 | ✅ 可选 | `@NotNull`, `@Size` (Bean Validation) |
| API 文档 | ✅ 可选 | `@Operation`, `@Tag` (Swagger) |

**反对的是**：Spring 式注解泛滥，一个类堆十几个注解  
**接受的是**：数据层/基础设施层的标准注解，简单明确

---

## 贡献

欢迎提交 Issue 和 PR！

## License

MIT
