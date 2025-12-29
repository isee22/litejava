# Spring Boot 迁移指南

## 最简迁移方案：使用 Spring MVC 注解

如果你想保留 Spring MVC 的注解风格（`@RestController`、`@GetMapping` 等），LiteJava 提供了 `SpringMvcAnnotationPlugin`，**几乎不需要改动 Controller 代码**。

参考示例：`litejava-examples/litejava-example-springboot`

### 启动类

```java
App app = LiteJava.create();

// 数据库
app.use(new HikariPlugin());
app.use(new MyBatisPlugin(hikari));

// 缓存 + @Cacheable 支持
app.use(new MemoryCachePlugin());
app.use(new SpringCachePlugin(cache));

// DI (Guice)
app.use(new GuicePlugin());

// Spring MVC 注解路由 - 关键！
SpringMvcAnnotationPlugin springMvc = new SpringMvcAnnotationPlugin();
springMvc.packages = "com.example.controller";
app.use(springMvc);

app.run();
```

### Controller 保持不变

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Inject  // 用 Guice 的 @Inject 替代 @Autowired
    private UserService userService;
    
    @GetMapping
    public List<User> list() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @PostMapping
    public User create(@RequestBody User user) {
        return userService.create(user);
    }
}
```

### 支持的注解

| Spring 注解 | 支持情况 |
|-------------|----------|
| `@RestController` | ✅ |
| `@Controller` | ✅ |
| `@RequestMapping` | ✅ |
| `@GetMapping` | ✅ |
| `@PostMapping` | ✅ |
| `@PutMapping` | ✅ |
| `@DeleteMapping` | ✅ |
| `@PathVariable` | ✅ |
| `@RequestParam` | ✅ |
| `@RequestBody` | ✅ |
| `@Cacheable` | ✅ (需要 SpringCachePlugin) |
| `@CachePut` | ✅ |
| `@CacheEvict` | ✅ |

### 迁移步骤

1. **替换依赖** - 移除 spring-boot-starter，添加 litejava-plugins
2. **改启动类** - 用 `LiteJava.create()` + 插件替代 `@SpringBootApplication`
3. **DI 替换** - `@Autowired` → `@Inject` (Guice)
4. **配置文件** - 调整 YAML 格式
5. **Controller 基本不变** - 注解继续用

这是最省力的迁移方式，适合想快速迁移又不想大改代码的场景。

---

## 完全迁移：LiteJava 原生风格

如果你想完全拥抱 LiteJava 的"代码即配置"风格，以下是详细对照。

## 概念对照

| Spring Boot | LiteJava | 说明 |
|-------------|----------|------|
| `@SpringBootApplication` | `LiteJava.create()` | 应用入口 |
| `@RestController` | Controller 类 | 无需注解 |
| `@GetMapping` | `app.get()` | 代码定义路由 |
| `@Autowired` | 直接访问 `app.xxx` | 无 DI 容器 |
| `@Service` | 普通类 | 无需注解 |
| `@Configuration` | `application.yml` | 配置文件 |
| `@Bean` | `app.use(plugin)` | 插件注册 |
| `Filter` | `MiddlewarePlugin` | 中间件 |
| `application.properties` | `application.yml` | 配置文件 |

## 启动类迁移

### Spring Boot

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### LiteJava

```java
public class Application {
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 注册插件
        app.use(new HikariPlugin());
        app.use(new MyBatisPlugin("com.example.mapper"));
        
        // 注册路由
        app.use(new UserController().routes());
        
        app.run();
    }
}
```

## Controller 迁移

### Spring Boot

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public List<User> list(@RequestParam(defaultValue = "1") int page) {
        return userService.list(page);
    }
    
    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @PostMapping
    public User create(@RequestBody UserDTO dto) {
        return userService.create(dto);
    }
}
```

### LiteJava

```java
public class UserController {
    
    public Routes routes() {
        return new Routes(this)
            .get("/api/users", this::list)
            .get("/api/users/:id", this::get)
            .post("/api/users", this::create)
            .end();
    }
    
    void list(Context ctx) {
        int page = ctx.queryParamInt("page", 1);
        ctx.ok(Service.user.list(page));
    }
    
    void get(Context ctx) {
        long id = ctx.pathParamLong("id");
        ctx.ok(Service.user.findById(id));
    }
    
    void create(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        ctx.ok(Service.user.create(body));
    }
}
```

## Service 迁移

### Spring Boot

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public User findById(Long id) {
        return userMapper.findById(id);
    }
}
```

### LiteJava

```java
public class UserService {
    
    public User findById(long id) {
        return mapper().findById(id);
    }
    
    UserMapper mapper() {
        return LiteJava.app.mybatis.getMapper(UserMapper.class);
    }
}

// 单例访问
public class Service {
    public static UserService user = new UserService();
}
```

## 配置迁移

### Spring Boot

```yaml
server:
  port: 8080
  
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 20
```

### LiteJava

```yaml
server:
  port: 8080
  
hikari:
  jdbcUrl: jdbc:mysql://localhost:3306/mydb
  username: root
  password: 123456
  maximumPoolSize: 20
```

## 拦截器迁移

### Spring Boot

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
```

### LiteJava

```java
public class AuthMiddleware extends MiddlewarePlugin {
    
    @Override
    public void handle(Context ctx) {
        // 排除路径
        if (ctx.path.startsWith("/api/auth")) {
            ctx.next();
            return;
        }
        
        String token = ctx.header("Authorization");
        if (token == null) {
            ctx.fail(401, -1, "请先登录");
            return;
        }
        
        ctx.next();
    }
}

// 注册
app.use("/api/*", new AuthMiddleware());
```

## 异常处理迁移

### Spring Boot

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBizException(BusinessException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("code", e.getCode(), "msg", e.getMessage()));
    }
}
```

### LiteJava

```java
// 内置 ExceptionPlugin 自动处理
// 抛出 LiteJavaException 会自动转换为 JSON 响应

void get(Context ctx) {
    User user = Service.user.findById(id);
    if (user == null) {
        ctx.error(404, "用户不存在");  // 抛出异常
    }
    ctx.ok(user);
}
```

## 响应格式迁移

### Spring Boot

```java
@Data
public class Result<T> {
    private int code;
    private T data;
    private String msg;
    
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.data = data;
        r.msg = "success";
        return r;
    }
}

@GetMapping("/{id}")
public Result<User> get(@PathVariable Long id) {
    return Result.ok(userService.findById(id));
}
```

### LiteJava

```java
// 内置统一响应格式
void get(Context ctx) {
    long id = ctx.pathParamLong("id");
    ctx.ok(Service.user.findById(id));
    // 自动返回 {"code": 0, "data": {...}, "msg": "success"}
}
```

## 常见问题

### 1. 没有依赖注入怎么办？

LiteJava 推荐简单的单例模式：

```java
public class Service {
    public static UserService user = new UserService();
    public static OrderService order = new OrderService();
}

// 使用
Service.user.findById(id);
```

### 2. 没有事务注解怎么办？

手动管理事务：

```java
public void transfer(long from, long to, int amount) {
    SqlSession session = app.mybatis.openSession();
    try {
        AccountMapper mapper = session.getMapper(AccountMapper.class);
        mapper.decrease(from, amount);
        mapper.increase(to, amount);
        session.commit();
    } catch (Exception e) {
        session.rollback();
        throw e;
    } finally {
        session.close();
    }
}
```

### 3. 没有参数校验注解怎么办？

手动校验或使用 Bean Validation：

```java
// 手动校验
void create(Context ctx) {
    Map<String, Object> body = ctx.bindJSON();
    String username = (String) body.get("username");
    if (username == null || username.trim().isEmpty()) {
        ctx.error(400, "用户名不能为空");
    }
}

// 或使用 Bean Validation
User user = ctx.bindJSON(User.class);
Set<ConstraintViolation<User>> violations = validator.validate(user);
if (!violations.isEmpty()) {
    ctx.error(400, violations.iterator().next().getMessage());
}
```

### 4. 迁移步骤建议

1. **先迁移配置文件** - 调整配置格式
2. **迁移 Entity** - 改为 public 字段
3. **迁移 Mapper** - 基本不变
4. **迁移 Service** - 去掉注解，改为单例
5. **迁移 Controller** - 改为 Routes 方式
6. **迁移拦截器** - 改为中间件
7. **测试验证** - 逐个接口测试

## 迁移方案对比

| 方案 | 改动量 | 适用场景 |
|------|--------|----------|
| **SpringMvcAnnotationPlugin** | 小 | 快速迁移，保留注解风格 |
| **完全迁移** | 大 | 新项目，追求极简风格 |

**推荐**：先用 `SpringMvcAnnotationPlugin` 快速迁移跑起来，后续逐步改造为原生风格。

## 示例项目

完整示例参考：

```
litejava/litejava-examples/litejava-example-springboot/
├── SpringBootApp.java           # 启动类
├── controller/
│   └── UserController.java      # @RestController 风格
├── service/
│   └── UserService.java         # @Inject + @Cacheable
├── mapper/
│   └── UserMapper.java          # MyBatis Mapper
└── model/
    └── User.java                # Entity
```

运行：

```bash
cd litejava-examples/litejava-example-springboot
mvn package -DskipTests
java -jar target/litejava-example-springboot-1.0.0-jdk8-shaded.jar
```
