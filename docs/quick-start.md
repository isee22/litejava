# LiteJava 快速入门

## 5 分钟上手

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.litejava</groupId>
    <artifactId>litejava-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Hello World

```java
import litejava.*;
import litejava.util.Maps;

public class App {
    public static void main(String[] args) {
        App app = LiteJava.create();
        app.get("/", ctx -> ctx.ok(Maps.of("msg", "Hello LiteJava!")));
        app.run();
    }
}
```

启动后访问 http://localhost:8080

### 3. 路由

```java
// 基本路由
app.get("/users", this::listUsers);
app.post("/users", this::createUser);
app.put("/users/:id", this::updateUser);
app.delete("/users/:id", this::deleteUser);

// 路径参数
app.get("/users/:id", ctx -> {
    long id = ctx.pathParamLong("id");
    ctx.ok(userService.findById(id));
});

// 查询参数
app.get("/search", ctx -> {
    String q = ctx.queryParam("q");
    int page = ctx.queryParamInt("page", 1);
    ctx.ok(searchService.search(q, page));
});

// 路由分组
app.group("/api/v1", g -> {
    g.get("/users", this::listUsers);
    g.post("/users", this::createUser);
});
```

### 4. 请求处理

```java
// 获取 JSON Body
Map<String, Object> body = ctx.bindJSON();
User user = ctx.bindJSON(User.class);

// 获取表单
String username = ctx.formParam("username");

// 获取文件
UploadedFile file = ctx.file("avatar");

// 获取 Header
String token = ctx.header("Authorization");

// 获取 Cookie
String sessionId = ctx.cookie("session_id");
```

### 5. 响应

```java
// JSON 响应
ctx.json(user);

// 统一格式响应
ctx.ok(data);           // {"code": 0, "data": data, "msg": "success"}
ctx.fail("错误信息");    // {"code": -1, "data": null, "msg": "错误信息"}

// 其他响应
ctx.text("Hello");
ctx.html("<h1>Hello</h1>");
ctx.redirect("/login");
ctx.file(bytes, "report.pdf");
```

### 6. 中间件

```java
// 全局中间件
app.use(ctx -> {
    long start = System.currentTimeMillis();
    ctx.next();
    System.out.println("耗时: " + (System.currentTimeMillis() - start) + "ms");
});

// 认证中间件
app.use("/api/*", ctx -> {
    String token = ctx.header("Authorization");
    if (token == null) {
        ctx.fail(401, -1, "请先登录");
        return;
    }
    ctx.state.put("userId", JwtUtil.verify(token));
    ctx.next();
});
```

### 7. 配置文件

创建 `src/main/resources/application.yml`:

```yaml
server:
  port: 8080
  
database:
  url: jdbc:mysql://localhost:3306/mydb
  username: root
  password: 123456
```

读取配置:

```java
int port = app.conf.getInt("server", "port", 8080);
String dbUrl = app.conf.getString("database", "url", "");
```

### 8. 数据库

```java
// 使用 MyBatis
app.use(new HikariPlugin());
app.use(new MyBatisPlugin("litex.mapper"));

// 在 Handler 中使用
UserMapper mapper = app.mybatis.getMapper(UserMapper.class);
User user = mapper.findById(1L);
```

### 9. 完整示例

```java
public class Application {
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 数据库
        app.use(new HikariPlugin());
        app.use(new MyBatisPlugin("com.example.mapper"));
        
        // CORS
        app.use(new CorsPlugin());
        
        // 路由
        app.use(new UserController().routes());
        app.use(new OrderController().routes());
        
        // Swagger
        app.use(new SwaggerPlugin("My API", "1.0.0"));
        
        app.run();
    }
}
```

## 下一步

- [插件开发指南](plugin-guide.md)
- [最佳实践](best-practices.md)
- [性能调优](performance.md)
