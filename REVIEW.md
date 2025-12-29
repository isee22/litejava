# LiteJava 框架评估与改进方向

## 与主流框架对比

| 维度 | LiteJava | Spring Boot | Vert.x | Javalin |
|------|----------|-------------|--------|---------|
| 启动速度 | ⭐⭐⭐⭐⭐ <500ms | ⭐⭐ 3-10s | ⭐⭐⭐⭐ ~1s | ⭐⭐⭐⭐ ~1s |
| 内存占用 | ⭐⭐⭐⭐⭐ 30-80MB | ⭐⭐ 200-500MB | ⭐⭐⭐ 100-200MB | ⭐⭐⭐⭐ 50-100MB |
| 学习曲线 | ⭐⭐⭐⭐⭐ 极低 | ⭐⭐ 陡峭 | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐ 低 |
| 生态/插件 | ⭐⭐⭐ 基础完善 | ⭐⭐⭐⭐⭐ 极丰富 | ⭐⭐⭐⭐ 丰富 | ⭐⭐⭐ 一般 |
| 生产验证 | ⭐ 新框架 | ⭐⭐⭐⭐⭐ 大量 | ⭐⭐⭐⭐ 较多 | ⭐⭐⭐ 中等 |
| 文档完善度 | ⭐⭐ 待完善 | ⭐⭐⭐⭐⭐ 极完善 | ⭐⭐⭐⭐ 完善 | ⭐⭐⭐⭐ 完善 |

## 优点

1. **真正的轻量** - core 模块零依赖，比 Javalin（依赖 Jetty）更纯粹
2. **代码即配置** - 路由/中间件全代码定义，IDE 友好，重构安全
3. **Go 风格** - public 字段、显式配置，对 Go 开发者友好
4. **插件架构清晰** - 洋葱模型中间件，插件生命周期明确
5. **启动极快** - 适合 Serverless/容器场景
6. **Swagger 自动生成** - 从路由表 + 注解自动生成 OpenAPI 文档

## 已完成功能

### 核心插件
- [x] 路由 (RouterPlugin) - Radix Tree 实现
- [x] 配置 (ConfPlugin / YamlConfPlugin)
- [x] 日志 (LogPlugin / Slf4jLogPlugin)
- [x] JSON (JsonPlugin / JacksonPlugin)
- [x] 静态文件 (StaticFilePlugin)
- [x] 异常处理 (ExceptionPlugin)
- [x] CORS (CorsPlugin)
- [x] 健康检查 (HealthPlugin)

### 数据库
- [x] HikariCP 连接池 (HikariPlugin)
- [x] MyBatis (MyBatisPlugin)
- [x] Hibernate/JPA (HibernatePlugin)

### 服务器
- [x] JDK HttpServer (HttpServerPlugin)
- [x] Netty (NettyServerPlugin)
- [x] Undertow (UndertowServerPlugin)

### 文档
- [x] Swagger UI (SwaggerPlugin)

### 其他
- [x] WebSocket 支持
- [x] 限流 (RateLimitPlugin)

## 待改进

### 1. 响应方法多次调用 ✅ 已解决

~~**现状**：`ctx.ok()` 多次调用静默覆盖，可能导致 bug 难排查~~

**已实现**：
- [x] 添加 `responded` 标志位
- [x] 多次响应打印警告日志
- [x] 提供 `ctx.isResponded()` 方法供用户检查

### 2. 配置优先级规则

**现状**：配置文件 > 构造参数 > 默认值，需要记忆

**建议**：
- [ ] 文档中突出说明这一规则
- [ ] 考虑提供 `@Required` 注解标记必须从配置读取的字段

### 3. 生态建设

**待补充插件**：
- [ ] 熔断插件 (CircuitBreakerPlugin)
- [ ] 链路追踪 (TracingPlugin) - OpenTelemetry
- [ ] 指标监控 (MetricsPlugin) - Micrometer
- [ ] GraphQL 支持
- [ ] gRPC 支持

### 4. 文档完善 ✅ 已完成

- [x] [快速入门教程](docs/quick-start.md)
- [x] [插件开发指南](docs/plugin-guide.md)
- [x] [最佳实践文档](docs/best-practices.md)
- [x] [性能调优指南](docs/performance.md)
- [x] [Spring Boot 迁移指南](docs/spring-migration.md)

## 适用场景

**适合**：
- 各种规模的 API 服务、微服务
- 对启动速度/内存敏感的场景（Serverless、边缘计算）
- 厌倦 Spring 注解地狱的开发者
- 想用原生 Java 方式集成第三方库
- 新项目、快速原型

**相比 Spring Boot 的优势**：
- **零学习成本** - 就是普通 Java 代码，没有框架魔法
- **真正的生态兼容** - 第三方库直接 new 使用，不需要 spring-boot-starter-xxx 桥接
- **代码透明** - 出问题看堆栈就能定位，不用猜框架在干什么
- **所见即所得** - 不需要学习"怎么配置"、"配置文件放哪"、"为什么不生效"

> **关于"Spring 生态丰富"的真相**：
> 
> Spring 的"生态"本质是 Spring + starter + 第三方库，而 starter 只是桥接代码，把第三方库包装成 Spring 风格。
> 
> LiteJava 直接用第三方库原生 API，不需要桥接层：
> ```java
> // Spring 方式：需要 spring-boot-starter-redis + 配置 + @Autowired
> @Autowired
> private RedisTemplate<String, Object> redisTemplate;
> 
> // LiteJava 方式：直接用 Jedis
> Jedis jedis = new Jedis("localhost", 6379);
> jedis.set("key", "value");
> ```
> 
> Java 生态的库本来就是给所有 Java 程序用的，Spring 反而是把它们"圈"进自己的体系。

## 定位

LiteJava 是一个**有态度的框架**，不是要取代 Spring Boot，而是给追求简洁、厌倦"魔法"的开发者一个选择。类似 Go 生态里 Gin 的定位——不是最强大的，但足够简单直接。
