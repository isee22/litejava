# LiteJava Spring Boot Style Example

用 LiteJava 插件构建类似 Spring Boot 的完整 Web 应用。

## 两种启动方式

| 方式 | 启动类 | 说明 |
|------|--------|------|
| 代码配置 | `SpringBootApp` | 代码中显式 `app.use(plugin)` |
| 纯配置 | `SpringBootConfApp` | 通过 `application.yml` 的 `plugins` 节点配置 |

## Spring Boot vs LiteJava 对照表

| 功能 | Spring Boot | LiteJava |
|------|-------------|----------|
| **Web 框架** | Spring MVC | `SpringMvcAnnotationPlugin` |
| **依赖注入** | Spring DI (@Autowired) | Guice (@Inject) |
| **ORM** | Spring Data JPA / MyBatis | `MyBatisPlugin` / `HibernatePlugin` |
| **数据源** | HikariCP (内置) | `HikariPlugin` / `DruidPlugin` |
| **缓存** | Spring Cache | `MemoryCachePlugin` / `RedisPlugin` |
| **定时任务** | @Scheduled | `SchedulePlugin` (Quartz) |
| **模板引擎** | Thymeleaf | `ThymeleafPlugin` |
| **异常处理** | @ControllerAdvice | `RecoveryPlugin` |
| **配置文件** | application.yml | `YamlConfPlugin` (LiteJava.create() 自带) |
| **JSON** | Jackson (内置) | `JacksonPlugin` (LiteJava.create() 自带) |

## 项目结构

```
src/main/java/example/
├── SpringBootApp.java       # 启动类
├── config/
│   └── AppModule.java       # Guice DI 配置
├── controller/
│   ├── UserController.java  # REST API (@RestController)
│   └── PageController.java  # 页面渲染 + 重定向
├── service/
│   └── UserService.java     # 业务逻辑 (@Singleton)
├── mapper/
│   └── UserMapper.java      # MyBatis Mapper
├── model/
│   └── User.java            # 实体类 (public 字段)
└── scheduler/
    └── TaskScheduler.java   # 定时任务

src/main/resources/
├── application.yml          # 配置文件
└── templates/
    └── index.html           # Thymeleaf 模板
```

## 核心代码示例

### 启动类
```java
App app = LiteJava.create();

// 全局异常处理
app.use(RecoveryPlugin.withStack());

// 模板引擎
app.use(new ThymeleafPlugin("templates/"));

// 数据库
app.use(new HikariPlugin());
app.use(new MyBatisPlugin(hikari, UserMapper.class));

// 缓存
app.use(new MemoryCachePlugin());

// 定时任务
app.use(new SchedulePlugin());

// Spring MVC 注解
SpringMvcAnnotationPlugin springMvc = new SpringMvcAnnotationPlugin();
springMvc.instanceProvider = injector::getInstance;
springMvc.packages = "example.controller";
app.use(springMvc);

app.run();
```

### Controller (Spring MVC 注解)
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Inject
    private UserService userService;
    
    @GetMapping
    public Map<String, Object> list(Context ctx) {
        int page = ctx.queryInt("page", 1);
        return Map.of("list", userService.findAll());
    }
    
    @GetMapping("/{id}")
    public Object get(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new LiteJavaException(404, "User not found");
        }
        return user;
    }
}
```

### Context 常用方法
```java
// 获取参数
ctx.query("name")           // 查询参数 ?name=xxx
ctx.queryInt("page", 1)     // 带默认值
ctx.param("id")             // 路径参数 /users/:id
ctx.body(User.class)        // 请求体

// 响应
ctx.json(data)              // JSON 响应
ctx.render("index", data)   // 渲染模板
ctx.redirect("/")           // 重定向
ctx.status(201)             // 设置状态码
ctx.header("X-Custom", "v") // 设置响应头
```

### 定时任务
```java
@Singleton
public class TaskScheduler {
    @Inject
    private SchedulePlugin schedule;
    
    public void start() {
        // cron: 秒 分 时 日 月 周
        schedule.cron("0 * * * * ?", this::everyMinute);
        schedule.cron("0 0 2 * * ?", this::dailyAt2AM);
    }
}
```

## 运行

```bash
mvn package -DskipTests

# 代码配置方式
java -cp target/litejava-example-springboot-1.0.0-jdk8-shaded.jar example.SpringBootApp

# 纯配置方式
java -cp target/litejava-example-springboot-1.0.0-jdk8-shaded.jar example.SpringBootConfApp
```

## 测试 API

```bash
# 健康检查
curl http://localhost:8080/health

# 用户列表
curl http://localhost:8080/api/users

# 创建用户
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Tom","email":"tom@test.com"}'

# 获取用户
curl http://localhost:8080/api/users/1

# 更新用户
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Tom Updated"}'

# 删除用户
curl -X DELETE http://localhost:8080/api/users/1

# 页面 (Thymeleaf)
curl http://localhost:8080/
```

## 配置文件 (application.yml)

```yaml
server:
  port: 8080

datasource:
  url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'
  username: sa
  password: ""
  driver: org.h2.Driver

scheduler:
  enabled: true

# 纯配置方式启动时使用
plugins:
  litejava_plugins_http_RecoveryPlugin:
    enabled: true
    showStack: true
  litejava_plugins_view_ThymeleafPlugin:
    enabled: true
    templateDir: templates/
  litejava_plugins_dataSource_HikariPlugin: true
  litejava_plugins_cache_MemoryCachePlugin: true
  litejava_plugins_schedule_SchedulePlugin: true
  litejava_plugins_annotation_SpringMvcAnnotationPlugin:
    enabled: true
    packages: example.controller
```
