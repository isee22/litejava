# LiteJava 功能开发文档

> Java 版的 Gin - 轻量级 Web 框架

## 进度说明

- ✅ 已完成
- 🚧 部分完成
- ❌ 未实现

---

## 一、核心模块 (litejava-core)

零外部依赖，纯 JDK 实现。

| 功能 | 状态 | 说明 |
|------|------|------|
| App 应用容器 | ✅ | 插件管理、生命周期、优雅停机 |
| Context 请求上下文 | ✅ | 请求/响应封装、参数绑定、Cookie |
| Handler 处理器 | ✅ | 函数式接口 |
| Plugin 插件基类 | ✅ | config/uninstall 生命周期 |
| MiddlewarePlugin 中间件 | ✅ | Koa-style 洋葱模型 |
| Route 路由定义 | ✅ | 支持元数据（summary/tags/params） |
| Routes 路由收集器 | ✅ | 批量注册路由 |
| UploadedFile 文件上传 | ✅ | multipart/form-data 解析 |

### 内置插件 (plugin/)

| 插件 | 状态 | 说明 |
|------|------|------|
| RouterPlugin | ✅ | Radix Tree 路由，支持 :param 和 *wildcard |
| ConfPlugin | ✅ | .properties 配置读取 |
| LogPlugin | ✅ | 简单日志（System.out） |
| HttpServerPlugin | ✅ | 基于 JDK HttpServer |
| ServerPlugin | ✅ | 服务器基类 |
| JsonPlugin | ✅ | JSON 基类（需子类实现） |
| ViewPlugin | ✅ | 视图/模板基类 |
| StaticFilePlugin | ✅ | 静态文件服务 |
| FilePlugin | ✅ | 文件上传处理 |
| BannerPlugin | ✅ | 启动 Banner |

### 异常处理 (exception/)

| 类 | 状态 | 说明 |
|------|------|------|
| LiteJavaException | ✅ | 框架基础异常，支持 HTTP 状态码 |

---

## 二、可选插件模块 (litejava-plugins)

有外部依赖，按需引入。

### 快速启动

| 类 | 状态 | 说明 |
|------|------|------|
| LiteJava.create() | ✅ | 预装常用插件，开箱即用 |
| AutoConfigPlugin | ✅ | 根据配置文件自动启用插件 |
| DebugPlugin | ✅ | 调试信息 |

### 配置 (config/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| YamlConfPlugin | ✅ | SnakeYAML | YAML 配置，支持多环境 |

### JSON (json/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| JacksonPlugin | ✅ | Jackson | JSON 序列化/反序列化 |

### 日志 (log/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| Slf4jLogPlugin | ✅ | SLF4J | SLF4J 日志适配 |
| RequestLogPlugin | ✅ | - | 请求日志中间件 |

### 数据源 (dataSource/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| HikariPlugin | ✅ | HikariCP | HikariCP 连接池（推荐） |
| DruidPlugin | ✅ | Druid | Druid 连接池 |

### 数据库 (database/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| JdbcPlugin | ✅ | Spring JDBC | JdbcTemplate 封装 |
| MyBatisPlugin | ✅ | MyBatis | SQL 映射框架 |
| JpaPlugin | ✅ | Hibernate | JPA ORM |
| HibernatePlugin | ✅ | Hibernate | Hibernate ORM |

### 缓存 (cache/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| CachePlugin | ✅ | - | 缓存基类 |
| MemoryCachePlugin | ✅ | - | 内存缓存（ConcurrentHashMap） |
| RedisCachePlugin | ✅ | Jedis | Redis 缓存，支持 Hash/List/Set |
| MemcacheCachePlugin | ✅ | spymemcached | Memcached 缓存 |

### 安全 (security/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| CorsPlugin | ✅ | - | 跨域中间件 |
| JwtPlugin | ✅ | jjwt | JWT 认证 |
| AuthPlugin | ✅ | - | 认证中间件基类 |
| BasicAuthPlugin | ✅ | - | HTTP Basic 认证 |
| SessionPlugin | ✅ | - | Session 管理 |
| CsrfPlugin | ✅ | - | CSRF 防护 |
| RateLimitPlugin | ✅ | - | 限流中间件 |
| OAuth2Plugin | 🚧 | - | OAuth2 认证（基础实现） |

### HTTP (http/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| RecoveryPlugin | ✅ | - | 异常恢复中间件 |
| GzipPlugin | ✅ | - | Gzip 压缩 |
| RequestIdPlugin | ✅ | - | 请求 ID 生成 |
| SsePlugin | ✅ | - | Server-Sent Events |

### 服务器 (server/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| NettyServerPlugin | ✅ | Netty | Netty 高性能服务器 |
| UndertowServerPlugin | ✅ | Undertow | Undertow 服务器 |
| JettyServerPlugin | ✅ | Jetty | Jetty 服务器 |

### 路由扩展 (router/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| SimpleRouterPlugin | ✅ | - | 简单路由（HashMap） |
| RegexRouterPlugin | ✅ | - | 正则路由 |
| TrieRouterPlugin | ✅ | - | Trie 树路由 |

### 注解路由 (annotation/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| JaxRsAnnotationPlugin | ✅ | JAX-RS | JAX-RS 注解路由 |
| JerseyRuntimePlugin | ✅ | Jersey | Jersey 运行时 |
| SpringMvcAnnotationPlugin | ✅ | Spring Web | Spring MVC 注解路由 |

### 模板引擎 (view/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| ThymeleafPlugin | ✅ | Thymeleaf | Thymeleaf 模板 |
| FreemarkerPlugin | ✅ | Freemarker | Freemarker 模板 |
| PebblePlugin | ✅ | Pebble | Pebble 模板 |

### 健康检查 (health/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| HealthPlugin | ✅ | - | /health 端点 |

### 监控指标 (metrics/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| MetricsPlugin | ✅ | Micrometer | Prometheus 指标 |

### 定时任务 (schedule/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| SchedulePlugin | ✅ | Quartz | Cron 定时任务 |

### 参数校验 (validation/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| ValidationPlugin | ✅ | Hibernate Validator | JSR-380 Bean Validation |

### 国际化 (i18n/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| I18nPlugin | ✅ | - | 多语言支持 |

### 依赖注入 (di/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| GuicePlugin | ✅ | Google Guice | DI 容器 |

### WebSocket (websocket/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| WebSocketPlugin | ✅ | - | WebSocket 服务器，支持房间 |

### API 文档 (doc/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| SwaggerPlugin | ✅ | Swagger | OpenAPI 文档生成 |

### GraphQL (graphql/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| GraphQLPlugin | ✅ | graphql-java | GraphQL 支持 |

### gRPC (grpc/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| GrpcPlugin | ✅ | gRPC | gRPC 服务 |

### 链路追踪 (tracing/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| TracingPlugin | ✅ | OpenTelemetry | 分布式追踪 |

### 热重载 (hotreload/)

| 插件 | 状态 | 依赖 | 说明 |
|------|------|------|------|
| HotReloadPlugin | ✅ | - | 开发模式热重载 |

---

## 三、虚拟线程模块 (litejava-plugins-vt)

Java 21+ 虚拟线程支持。

| 插件 | 状态 | 说明 |
|------|------|------|
| JdbcVirtualThreadPlugin | ✅ | 虚拟线程数据库连接池 |

---

## 四、核心特性总结

### 路由系统 ✅

- Radix Tree 高性能路由
- 路径参数 `:id`
- 通配符 `*filepath`
- 路由分组 `app.group("/api")`
- ANY 方法匹配
- 404/405 自定义处理

### 中间件 ✅

- Koa-style 洋葱模型
- 全局/分组/路由级中间件
- abort 机制中断请求

### 请求处理 ✅

- JSON/Form/Multipart 自动解析
- 类型安全的参数获取
- 文件上传
- Cookie 读写

### 响应输出 ✅

- JSON/HTML/Text/Binary
- 文件下载
- 重定向
- 模板渲染
- 统一响应格式 `ctx.ok()/ctx.fail()`

### 配置系统 ✅

- .properties / YAML
- 多环境配置
- 配置热加载

### 插件系统 ✅

- 生命周期管理
- 自动类型注册
- 插件依赖

---

## 五、待开发功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 集群支持 | 中 | 多实例部署、Session 共享 |
| 配置中心 | 中 | Nacos/Apollo 集成 |
| 服务发现 | 低 | Consul/Eureka 集成 |
| 消息队列 | 中 | Kafka/RabbitMQ 插件 |
| 全文搜索 | 低 | Elasticsearch 插件 |
| 文件存储 | 中 | S3/OSS 插件 |

---

## 六、版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | - | 核心功能完成 |

