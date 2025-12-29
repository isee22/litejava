# LiteJava 最佳实践

## 项目结构

```
src/main/java/com/example/
├── Application.java          # 启动类
├── controller/               # Controller 层
│   ├── UserController.java
│   └── OrderController.java
├── service/                  # Service 层
│   ├── UserService.java
│   └── OrderService.java
├── mapper/                   # MyBatis Mapper
│   ├── UserMapper.java
│   └── OrderMapper.java
├── entity/                   # 实体类
│   ├── User.java
│   └── Order.java
├── middleware/               # 自定义中间件
│   └── AuthMiddleware.java
└── plugin/                   # 自定义插件
    └── SmsPlugin.java

src/main/resources/
├── application.yml           # 配置文件
├── application-dev.yml       # 开发环境配置
├── application-prod.yml      # 生产环境配置
└── mapper/                   # MyBatis XML
    ├── UserMapper.xml
    └── OrderMapper.xml
```

## Controller 设计

### 推荐写法

```java
@Tag(name = "用户", description = "用户相关接口")
public class UserController {
    
    public Routes routes() {
        return new Routes(this)
            .get("/api/users", this::list)
            .get("/api/users/:id", this::get)
            .post("/api/users", this::create)
            .put("/api/users/:id", this::update)
            .delete("/api/users/:id", this::delete)
            .end();
    }
    
    @Operation(summary = "用户列表")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "页码")
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "每页数量")
    void list(Context ctx) {
        int page = ctx.queryParamInt("page", 1);
        int size = ctx.queryParamInt("size", 20);
        ctx.ok(Service.user.list(page, size));
    }
    
    @Operation(summary = "用户详情")
    void get(Context ctx) {
        long id = ctx.pathParamLong("id");
        User user = Service.user.findById(id);
        if (user == null) {
            ctx.error(404, "用户不存在");
        }
        ctx.ok(user);
    }
    
    @Operation(summary = "创建用户")
    void create(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        User user = Service.user.create(body);
        ctx.ok(user);
    }
}
```

### 要点

1. **一个 Controller 一个 routes()** - 路由集中管理
2. **使用 Swagger 注解** - 自动生成 API 文档
3. **Service 层处理业务** - Controller 只做参数解析和响应
4. **ctx.error() 抛异常** - 统一异常处理

## Service 设计

### 单例模式

```java
public class Service {
    public static UserService user = new UserService();
    public static OrderService order = new OrderService();
}

// 使用
User user = Service.user.findById(id);
```

### Service 实现

```java
public class UserService {
    
    public User findById(long id) {
        return mapper().findById(id);
    }
    
    public List<User> list(int page, int size) {
        int offset = (page - 1) * size;
        return mapper().list(offset, size);
    }
    
    public User create(Map<String, Object> data) {
        User user = new User();
        user.username = (String) data.get("username");
        user.email = (String) data.get("email");
        user.createdat = LocalDateTime.now();
        mapper().insert(user);
        return user;
    }
    
    UserMapper mapper() {
        return LiteJava.app.mybatis.getMapper(UserMapper.class);
    }
}
```

## Entity 设计

### 推荐写法

```java
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    public String username;
    public String email;
    public String password;
    public LocalDateTime createdat;
    
    // 不要用 @Transient 挂载关联对象
    // 关联数据在 Service 层用 Map 组装
}
```

### 要点

1. **public 字段** - 不用 getter/setter
2. **字段名全小写** - 与数据库列名一致
3. **不挂载关联对象** - 保持 Entity 纯粹
4. **关联数据用 Map 返回**

```java
// Service 层组装关联数据
public Map<String, Object> getUserWithOrders(long userId) {
    User user = userMapper.findById(userId);
    List<Order> orders = orderMapper.findByUserId(userId);
    return Maps.of("user", user, "orders", orders);
}
```

## 配置管理

### 多环境配置

```yaml
# application.yml - 公共配置
server:
  port: 8080

# application-dev.yml - 开发环境
database:
  url: jdbc:mysql://localhost:3306/mydb_dev
  
# application-prod.yml - 生产环境  
database:
  url: jdbc:mysql://prod-db:3306/mydb
```

启动时指定环境：

```bash
java -jar app.jar --profile=prod
```

### 敏感配置

```yaml
# 敏感信息用环境变量
database:
  password: ${DB_PASSWORD}
  
jwt:
  secret: ${JWT_SECRET}
```

## 异常处理

### 使用 ctx.error()

```java
void get(Context ctx) {
    long id = ctx.pathParamLong("id");
    
    // 参数校验
    if (id <= 0) {
        ctx.error(400, "无效的 ID");
    }
    
    // 业务校验
    User user = Service.user.findById(id);
    if (user == null) {
        ctx.error(404, "用户不存在");
    }
    
    ctx.ok(user);
}
```

### 自定义异常

```java
public class BizException extends LiteJavaException {
    public int code;
    
    public BizException(int code, String msg) {
        super(msg, 400);
        this.code = code;
    }
}

// 使用
throw new BizException(1001, "余额不足");
```

## 认证授权

### JWT 认证中间件

```java
public class AuthMiddleware extends MiddlewarePlugin {
    public List<String> excludePaths = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register"
    );
    
    @Override
    public void handle(Context ctx) {
        // 排除路径
        if (excludePaths.contains(ctx.path)) {
            ctx.next();
            return;
        }
        
        // 验证 token
        String token = ctx.header("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            ctx.fail(401, -1, "请先登录");
            return;
        }
        
        try {
            Long userId = JwtUtil.verify(token.substring(7));
            ctx.state.put("userId", userId);
            ctx.next();
        } catch (Exception e) {
            ctx.fail(401, -1, "登录已过期");
        }
    }
}
```

### 获取当前用户

```java
void update(Context ctx) {
    Long userId = (Long) ctx.state.get("userId");
    // ...
}
```

## 日志规范

```java
// 使用 app.log
app.log.info("用户登录: {}", username);
app.log.warn("登录失败: {}", reason);
app.log.error("系统异常", e);

// 不要用 System.out.println
```

## 响应格式

### 统一格式

```java
// 成功
ctx.ok(data);
// {"code": 0, "data": {...}, "msg": "success"}

// 失败
ctx.fail("错误信息");
// {"code": -1, "data": null, "msg": "错误信息"}

// 自定义错误码
ctx.fail(1001, "余额不足");
// {"code": 1001, "data": null, "msg": "余额不足"}
```

### 列表数据

```java
// 统一用 list 字段
ctx.ok(Maps.of("list", users, "total", total));
// {"code": 0, "data": {"list": [...], "total": 100}, "msg": "success"}
```

## 性能优化

1. **使用连接池** - HikariCP
2. **合理设置超时** - 数据库、HTTP 客户端
3. **避免 N+1 查询** - 批量查询
4. **使用缓存** - Redis/内存缓存
5. **异步处理** - 耗时操作异步化

详见 [性能调优指南](performance.md)
