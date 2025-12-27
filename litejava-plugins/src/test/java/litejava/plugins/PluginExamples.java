package litejava.plugins;

import litejava.App;
import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;
import litejava.plugin.StaticFilePlugin;
import litejava.plugins.cache.CachePlugin;
import litejava.plugins.cache.MemoryCachePlugin;
import litejava.plugins.cache.RedisCachePlugin;
import litejava.plugins.config.YamlConfPlugin;
import litejava.plugins.database.JdbcPlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.json.JacksonPlugin;
import litejava.plugins.log.RequestLogPlugin;
import litejava.plugins.security.*;

import java.util.Map;

/**
 * LiteJava 插件使用示例
 * 
 * <p>本文件展示各插件的基本用法，可直接复制到项目中使用。
 */
public class PluginExamples {

    /**
     * 示例 1: 最简单的应用
     */
    public static void simpleApp() {
        App app = LiteJava.create();  // 预装 YamlConf + Jackson + MemoryCache + HttpServer
        
        app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
        app.get("/users/:id", ctx -> ctx.ok(Map.of("id", ctx.pathParamLong("id"))));
        
        app.run();
    }

    /**
     * 示例 2: 手动配置插件
     */
    public static void manualConfig() {
        App app = new App();
        
        // 配置插件
        app.use(new YamlConfPlugin());      // YAML 配置
        app.use(new JacksonPlugin());       // JSON 处理
        app.use(new MemoryCachePlugin());   // 内存缓存
        
        // 中间件
        app.use(new RequestLogPlugin());    // 请求日志
        app.use(new RecoveryPlugin());      // 异常恢复
        
        // 路由
        app.get("/", ctx -> ctx.text("Hello"));
        
        app.run();
    }

    /**
     * 示例 3: 数据库 (JdbcPlugin)
     */
    public static void jdbcExample() {
        App app = LiteJava.create();
        app.use(new JdbcPlugin());
        
        app.get("/users", ctx -> {
            // 使用 JdbcTemplate 查询
            ctx.ok(JdbcPlugin.instance.jdbcTemplate.queryForList("SELECT * FROM users"));
        });
        
        app.get("/users/:id", ctx -> {
            long id = ctx.pathParamLong("id");
            ctx.ok(JdbcPlugin.instance.jdbcTemplate.queryForMap(
                "SELECT * FROM users WHERE id = ?", id));
        });
        
        app.post("/users", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            JdbcPlugin.instance.jdbcTemplate.update(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                body.get("name"), body.get("email"));
            ctx.ok("created");
        });
        
        app.run();
    }

    /**
     * 示例 4: 缓存 (CachePlugin)
     */
    public static void cacheExample() {
        App app = LiteJava.create();
        // app.use(new RedisCachePlugin());  // 生产环境用 Redis
        
        app.get("/users/:id", ctx -> {
            long id = ctx.pathParamLong("id");
            
            // 缓存获取或加载
            Object user = CachePlugin.instance.getOrLoad("user:" + id, () -> {
                // 模拟数据库查询
                return Map.of("id", id, "name", "User " + id);
            });
            
            ctx.ok(user);
        });
        
        app.delete("/users/:id/cache", ctx -> {
            long id = ctx.pathParamLong("id");
            CachePlugin.instance.del("user:" + id);
            ctx.ok("cache cleared");
        });
        
        app.run();
    }

    /**
     * 示例 5: Session 认证
     */
    public static void sessionExample() {
        App app = LiteJava.create();
        app.use(new SessionPlugin());
        
        app.post("/login", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            // 验证用户...
            
            // 存储到 session
            Map<String, Object> session = SessionPlugin.get(ctx);
            session.put("userId", 123);
            session.put("username", body.get("username"));
            
            ctx.ok("logged in");
        });
        
        app.get("/me", ctx -> {
            Map<String, Object> session = SessionPlugin.get(ctx);
            if (session.get("userId") == null) {
                ctx.fail(401, -1, "not logged in");
                return;
            }
            ctx.ok(session);
        });
        
        app.run();
    }

    /**
     * 示例 6: Token 认证 (AuthPlugin)
     */
    public static void authExample() {
        App app = LiteJava.create();
        
        // Token 认证
        app.use(new AuthPlugin(token -> {
            // 验证 token，返回用户信息或 null
            if ("valid-token".equals(token)) {
                return Map.of("userId", 123, "username", "admin");
            }
            return null;
        }).whitelist("/", "/login")
          .whitelistPrefix("/public"));
        
        app.get("/", ctx -> ctx.text("public"));
        
        app.post("/login", ctx -> {
            // 验证用户后返回 token
            ctx.ok(Map.of("token", "valid-token"));
        });
        
        app.get("/me", ctx -> {
            // 认证信息在 ctx.state.get("auth")
            ctx.ok(ctx.state.get("auth"));
        });
        
        app.run();
    }

    /**
     * 示例 7: JWT 认证
     */
    public static void jwtExample() {
        App app = LiteJava.create();
        app.use(new JwtPlugin("your-secret-key-at-least-32-chars"));
        
        app.post("/login", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            // 验证用户后生成 JWT
            String token = JwtPlugin.instance.sign(Map.of(
                "userId", 123,
                "username", body.get("username")
            ));
            ctx.ok(Map.of("token", token));
        });
        
        app.get("/me", ctx -> {
            String token = ctx.header("Authorization");
            if (token == null) {
                ctx.fail(401, -1, "no token");
                return;
            }
            
            try {
                Object claims = JwtPlugin.instance.verify(
                    token.replace("Bearer ", ""));
                ctx.ok(claims);
            } catch (Exception e) {
                ctx.fail(401, -1, "invalid token");
            }
        });
        
        app.run();
    }

    /**
     * 示例 8: CORS 跨域
     */
    public static void corsExample() {
        App app = LiteJava.create();
        
        // 允许所有来源
        app.use(new CorsPlugin());
        
        // 或自定义配置
        // CorsPlugin cors = new CorsPlugin();
        // cors.allowOrigins = "https://example.com";
        // cors.allowMethods = "GET,POST,PUT,DELETE";
        // app.use(cors);
        
        app.get("/api/data", ctx -> ctx.ok("data"));
        
        app.run();
    }

    /**
     * 示例 9: 限流
     */
    public static void rateLimitExample() {
        App app = LiteJava.create();
        
        // 每分钟最多 100 次请求
        app.use(new RateLimitPlugin(100, 60000));
        
        app.get("/api/data", ctx -> ctx.ok("data"));
        
        app.run();
    }

    /**
     * 示例 10: 静态文件
     */
    public static void staticFileExample() {
        App app = LiteJava.create();
        
        // /static/* -> classpath:static/
        app.use(new StaticFilePlugin("/static", "static"));
        
        // /files/* -> 文件系统 ./uploads/
        // app.use(new StaticFilePlugin("/files", new File("uploads")));
        
        app.get("/", ctx -> ctx.html("<a href='/static/index.html'>Static</a>"));
        
        app.run();
    }

    /**
     * 示例 11: 路由分组
     */
    public static void routeGroupExample() {
        App app = LiteJava.create();
        
        // API v1
        app.group("/api/v1", api -> {
            api.get("/users", ctx -> ctx.ok("v1 users"));
            api.get("/books", ctx -> ctx.ok("v1 books"));
        });
        
        // API v2
        app.group("/api/v2", api -> {
            api.get("/users", ctx -> ctx.ok("v2 users"));
        });
        
        // 管理后台
        app.group("/admin", admin -> {
            // 可以给分组添加中间件
            admin.get("/dashboard", ctx -> ctx.ok("dashboard"));
        });
        
        app.run();
    }

    /**
     * 示例 12: 中间件
     */
    public static void middlewareExample() {
        App app = LiteJava.create();
        
        // 自定义中间件 - 计时
        app.use(new MiddlewarePlugin() {
            @Override
            public void handle(Context ctx, Next next) throws Exception {
                long start = System.currentTimeMillis();
                next.run();
                long cost = System.currentTimeMillis() - start;
                ctx.header("X-Response-Time", cost + "ms");
            }
        });
        
        // 自定义中间件 - 认证
        app.use(new MiddlewarePlugin() {
            @Override
            public void handle(Context ctx, Next next) throws Exception {
                if (ctx.path.startsWith("/admin") && ctx.header("Token") == null) {
                    ctx.status(401).json(Map.of("error", "unauthorized"));
                    return;
                }
                next.run();
            }
        });
        
        app.get("/", ctx -> ctx.ok("public"));
        app.get("/admin/data", ctx -> ctx.ok("admin data"));
        
        app.run();
    }

    public static void main(String[] args) {
        System.out.println("=== LiteJava Plugin Examples ===");
        System.out.println("查看源码了解各插件用法");
        System.out.println("取消注释对应方法即可运行示例");
        
        // simpleApp();
        // manualConfig();
        // jdbcExample();
        // cacheExample();
        // sessionExample();
        // authExample();
        // jwtExample();
        // corsExample();
        // rateLimitExample();
        // staticFileExample();
        // routeGroupExample();
        // middlewareExample();
    }
}
