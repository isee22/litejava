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

## 快速开始

### Maven

```xml
<dependency>
    <groupId>litejava</groupId>
    <artifactId>litejava-plugins</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Hello World

```java
import litejava.*;
import litejava.plugins.LiteJava;

public class Main {
    public static void main(String[] args) {
        App app = LiteJava.create();
        app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
        app.get("/users/:id", ctx -> ctx.ok(Map.of("id", ctx.pathParam("id"))));
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
ctx.pathParam("id")       // 路径参数
ctx.queryParam("name")    // 查询参数
ctx.formParam("field")    // 表单参数
ctx.header("Token")       // 请求头
ctx.bindJSON()            // JSON 请求体

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

---

## 功能清单

### 核心模块 (litejava-core) - 零依赖

| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| 路由 | `RouterPlugin` | ✅ | Radix Tree 路由，支持分组、通配符、路径参数 |
| HTTP 服务器 | `HttpServerPlugin` | ✅ | 基于 JDK 内置 HttpServer |
| 配置 | `ConfPlugin` | ✅ | .properties 配置文件 |
| 日志 | `LogPlugin` | ✅ | 简单日志输出 |
| JSON | `JsonPlugin` | ✅ | 零依赖 JSON 解析/序列化 |
| 静态文件 | `StaticFilePlugin` | ✅ | 文件系统 + Classpath |
| 模板 | `ViewPlugin` | ✅ | 视图渲染基类 |
| Banner | `BannerPlugin` | ✅ | 启动 Banner |

### 可选插件 (litejava-plugins)

#### 配置
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| YAML 配置 | `YamlConfPlugin` | ✅ | YAML 格式配置文件 |

#### 服务器
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Netty | `NettyServerPlugin` | ✅ | 高性能 Netty 服务器 |
| Jetty | `JettyServerPlugin` | ✅ | Jetty 嵌入式服务器 |
| Undertow | `UndertowServerPlugin` | ✅ | Undertow 服务器 |

#### 数据库
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| JDBC | `JdbcPlugin` | ✅ | 原生 JDBC 封装 |
| JPA | `JpaPlugin` | ✅ | JPA 标准接口 |
| Hibernate | `HibernatePlugin` | ✅ | Hibernate ORM |
| MyBatis | `MyBatisPlugin` | ✅ | MyBatis 集成 |

#### 缓存
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| 内存缓存 | `MemoryCachePlugin` | ✅ | 本地内存缓存 |
| Redis | `RedisCachePlugin` | ✅ | Redis 缓存 |
| Memcached | `MemcacheCachePlugin` | ✅ | Memcached 缓存 |

#### 模板引擎
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Thymeleaf | `ThymeleafPlugin` | ✅ | Thymeleaf 模板 |
| Freemarker | `FreemarkerPlugin` | ✅ | Freemarker 模板 |
| Pebble | `PebblePlugin` | ✅ | Pebble 模板 |

#### JSON
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Jackson | `JacksonPlugin` | ✅ | Jackson JSON 处理 |

#### 安全
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| CORS | `CorsPlugin` | ✅ | 跨域资源共享 |
| JWT | `JwtPlugin` | ✅ | JWT 认证 |
| Session | `SessionPlugin` | ✅ | Session 管理 |
| CSRF | `CsrfPlugin` | ✅ | CSRF 防护 |
| Basic Auth | `BasicAuthPlugin` | ✅ | HTTP Basic 认证 |
| 限流 | `RateLimitPlugin` | ✅ | 请求限流 |

#### HTTP 增强
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| 异常恢复 | `RecoveryPlugin` | ✅ | 全局异常处理 |
| Gzip | `GzipPlugin` | ✅ | Gzip 压缩 |
| 请求ID | `RequestIdPlugin` | ✅ | 请求追踪 ID |
| SSE | `SsePlugin` | ✅ | Server-Sent Events |

#### 日志
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| 请求日志 | `RequestLogPlugin` | ✅ | HTTP 请求日志 |
| SLF4J | `Slf4jLogPlugin` | ✅ | SLF4J 日志集成 |

#### 路由扩展
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Spring MVC | `SpringMvcPlugin` | ✅ | Spring MVC 注解风格路由 |
| JAX-RS | `JaxRsPlugin` | ✅ | JAX-RS 注解风格路由 |
| Jersey | `JerseyPlugin` | ✅ | Jersey 集成 |
| 正则路由 | `RegexRouterPlugin` | ✅ | 正则表达式路由 |
| Trie 路由 | `TrieRouterPlugin` | ✅ | Trie 树路由 |
| 简单路由 | `SimpleRouterPlugin` | ✅ | 简单路由实现 |

#### 参数校验
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Bean Validation | `BeanValidationPlugin` | ✅ | JSR-380 Bean Validation |
| 自定义校验 | `ValidationPlugin` | ✅ | 轻量级参数校验 |

#### 依赖注入
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Guice | `GuicePlugin` | ✅ | Google Guice DI |

#### 定时任务
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| 调度器 | `SchedulePlugin` | ✅ | 定时任务调度 |

#### 国际化
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| I18n | `I18nPlugin` | ✅ | 多语言国际化 |

#### 健康检查
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Health | `HealthPlugin` | ✅ | 健康检查端点 |

#### API 文档
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Swagger | `SwaggerPlugin` | ✅ | OpenAPI/Swagger 文档 |

#### WebSocket
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| WebSocket | `WebSocketPlugin` | ✅ | WebSocket 支持 |

#### 可观测性
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| Metrics | `MetricsPlugin` | ✅ | Prometheus 风格指标 |
| 链路追踪 | `TracingPlugin` | ✅ | 分布式链路追踪 (OpenTelemetry/Jaeger/Zipkin) |

#### 开发工具
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| 热重载 | `HotReloadPlugin` | ✅ | 文件变化监控，配置热重载 |

#### API 协议
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| GraphQL | `GraphQLPlugin` | ✅ | GraphQL 查询支持 |
| gRPC | `GrpcPlugin` | ✅ | gRPC 服务支持 |

#### 认证扩展
| 功能 | 插件 | 状态 | 说明 |
|------|------|:----:|------|
| OAuth2 | `OAuth2Plugin` | ✅ | OAuth2 认证 (GitHub/Google/微信) |

---

## 待完善功能

| 功能 | 状态 | 说明 |
|------|:----:|------|
| CLI 脚手架 | ❌ | 项目生成工具 |

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
