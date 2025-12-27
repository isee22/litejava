package litejava;

import litejava.plugin.*;

/**
 * LiteJava Core 快速入门示例
 * 
 * <p>本文件展示 litejava-core 模块的核心功能，无需任何外部依赖。
 * 
 * <h2>litejava-core 提供的功能</h2>
 * <ul>
 *   <li>App - 应用容器，插件管理</li>
 *   <li>Context - 请求/响应上下文</li>
 *   <li>RouterPlugin - Gin-style 路由（参数、通配符、分组）</li>
 *   <li>MiddlewarePlugin - Koa-style 洋葱中间件</li>
 *   <li>ConfPlugin - .properties 配置</li>
 *   <li>LogPlugin - 简单日志</li>
 *   <li>ServerPlugin - 服务器基类（需实现）</li>
 *   <li>JsonPlugin - JSON 基类（需实现）</li>
 *   <li>ViewPlugin - 视图基类（需实现）</li>
 * </ul>
 * 
 * <h2>运行方式</h2>
 * <pre>{@code
 * mvn test -pl litejava-core -Dtest=QuickStartExample
 * }</pre>
 */
public class QuickStartExample {

    /**
     * 示例 1: 基础路由
     */
    public static void routingExample() {
        App app = new App();
        
        // GET 路由
        app.get("/", ctx -> ctx.text("Hello World"));
        app.get("/users", ctx -> ctx.text("User List"));
        
        // POST/PUT/DELETE
        app.post("/users", ctx -> ctx.text("Create User"));
        app.put("/users/:id", ctx -> ctx.text("Update User " + ctx.pathParam("id")));
        app.delete("/users/:id", ctx -> ctx.text("Delete User " + ctx.pathParam("id")));
        
        // 路径参数
        app.get("/users/:id", ctx -> {
            String id = ctx.pathParam("id");
            ctx.text("User: " + id);
        });
        
        // 多个参数
        app.get("/users/:userId/posts/:postId", ctx -> {
            String userId = ctx.pathParam("userId");
            String postId = ctx.pathParam("postId");
            ctx.text("User " + userId + ", Post " + postId);
        });
        
        // 通配符路由
        app.get("/files/*filepath", ctx -> {
            ctx.text("File: " + ctx.wildcardPath);
        });
        
        System.out.println("✓ 路由示例完成");
    }

    /**
     * 示例 2: 路由分组 (Gin-style)
     */
    public static void routeGroupExample() {
        App app = new App();
        
        // API 版本分组
        app.group("/api/v1", api -> {
            api.get("/users", ctx -> ctx.text("API v1 Users"));
            api.get("/books", ctx -> ctx.text("API v1 Books"));
        });
        
        // 嵌套分组
        app.group("/admin", admin -> {
            admin.group("/users", users -> {
                users.get("/", ctx -> ctx.text("Admin User List"));
                users.delete("/:id", ctx -> ctx.text("Delete User"));
            });
        });
        
        // 链式分组
        RouterPlugin api = app.group("/api/v2");
        api.get("/products", ctx -> ctx.text("Products"));
        api.post("/products", ctx -> ctx.text("Create Product"));
        
        System.out.println("✓ 路由分组示例完成");
    }

    /**
     * 示例 3: 中间件 (Koa-style 洋葱模型)
     */
    public static void middlewareExample() {
        App app = new App();
        
        // 日志中间件
        app.use(new MiddlewarePlugin() {
            @Override
            public void handle(Context ctx, Next next) throws Exception {
                long start = System.currentTimeMillis();
                System.out.println("→ " + ctx.method + " " + ctx.path);
                
                next.run();  // 执行后续中间件和 handler
                
                long cost = System.currentTimeMillis() - start;
                System.out.println("← " + ctx.getResponseStatus() + " (" + cost + "ms)");
            }
        });
        
        // 认证中间件
        app.use(new MiddlewarePlugin() {
            @Override
            public void handle(Context ctx, Next next) throws Exception {
                String token = ctx.header("Authorization");
                if (token == null && ctx.path.startsWith("/admin")) {
                    ctx.abortWithStatus(401);
                    ctx.text("Unauthorized");
                    return;
                }
                ctx.state.put("user", "admin");  // 传递数据给后续处理
                next.run();
            }
        });
        
        app.get("/admin/dashboard", ctx -> {
            String user = (String) ctx.state.get("user");
            ctx.text("Welcome, " + user);
        });
        
        System.out.println("✓ 中间件示例完成");
    }

    /**
     * 示例 4: Context 请求/响应
     */
    public static void contextExample() {
        Context ctx = new Context();
        ctx.app = new App();
        ctx.method = "POST";
        ctx.path = "/users";
        ctx.headers.put("Content-Type", "application/x-www-form-urlencoded");
        ctx.queryParams.put("page", "1");
        ctx.queryParams.put("size", "10");
        
        // 查询参数
        String page = ctx.queryParam("page");           // "1"
        Integer pageInt = ctx.queryParam("page", Integer.class);  // 1
        String missing = ctx.queryParam("sort", "id");  // "id" (默认值)
        
        // 路径参数
        ctx.params.put("id", "123");
        Long id = ctx.pathParam("id", Long.class);  // 123L
        
        // 响应
        ctx.status(201);
        ctx.header("X-Custom", "value");
        ctx.text("Created");
        
        // Cookie
        ctx.setCookie("session", "abc123", 3600);
        
        System.out.println("✓ Context 示例完成");
        System.out.println("  page=" + page + ", pageInt=" + pageInt + ", missing=" + missing + ", id=" + id);
    }

    /**
     * 示例 5: 配置插件
     */
    public static void configExample() {
        App app = new App();
        
        // 默认 ConfPlugin 支持 .properties 格式
        ConfPlugin conf = app.conf;
        
        // 获取配置（带默认值）
        int port = conf.getInt("server", "port", 8080);
        String env = conf.getString("app", "env", "dev");
        boolean debug = conf.getBool("app", "debug", true);
        
        System.out.println("✓ 配置示例完成");
        System.out.println("  port=" + port + ", env=" + env + ", debug=" + debug);
    }

    /**
     * 示例 6: 错误处理
     */
    public static void errorHandlingExample() {
        App app = new App();
        
        // 全局错误处理
        app.onError((ctx, e) -> {
            System.err.println("Error: " + e.getMessage());
            ctx.status(500).text("Internal Error");
        });
        
        // 404 处理
        app.noRoute(ctx -> {
            ctx.status(404).text("Page Not Found: " + ctx.path);
        });
        
        // 405 处理
        app.noMethod(ctx -> {
            ctx.status(405).text("Method Not Allowed: " + ctx.method);
        });
        
        System.out.println("✓ 错误处理示例完成");
    }

    /**
     * 示例 7: 插件系统
     */
    public static void pluginExample() {
        App app = new App();
        
        // 自定义插件
        Plugin myPlugin = new Plugin() {
            @Override
            public void config() {
                System.out.println("  MyPlugin configured");
            }
            
            @Override
            public void uninstall() {
                System.out.println("  MyPlugin uninstalled");
            }
        };
        
        app.use(myPlugin);
        app.unuse(myPlugin.getClass());
        
        System.out.println("✓ 插件系统示例完成");
    }

    /**
     * 示例 8: 完整应用结构
     */
    public static void fullAppExample() {
        App app = new App();
        app.port(8080).devMode(true).env("dev");
        
        // 中间件
        app.use(new RequestLogMiddleware());
        
        // 路由
        app.get("/", ctx -> ctx.text("Hello LiteJava!"));
        
        app.group("/api", api -> {
            api.get("/health", ctx -> ctx.text("OK"));
            api.get("/users/:id", ctx -> {
                ctx.text("User: " + ctx.pathParam("id"));
            });
        });
        
        // 错误处理
        app.noRoute(ctx -> ctx.status(404).text("Not Found"));
        
        // 启动前回调
        app.onReady(() -> System.out.println("  App ready!"));
        
        System.out.println("✓ 完整应用示例完成");
        System.out.println("  (需要注册 ServerPlugin 才能 app.run())");
    }

    // 示例中间件
    static class RequestLogMiddleware extends MiddlewarePlugin {
        @Override
        public void handle(Context ctx, Next next) throws Exception {
            System.out.println("  [LOG] " + ctx.method + " " + ctx.path);
            next.run();
        }
    }

    /**
     * 运行所有示例
     */
    public static void main(String[] args) {
        System.out.println("=== LiteJava Core 功能示例 ===\n");
        
        routingExample();
        routeGroupExample();
        middlewareExample();
        contextExample();
        configExample();
        errorHandlingExample();
        pluginExample();
        fullAppExample();
        
        System.out.println("\n=== 所有示例完成 ===");
        System.out.println("\n提示: litejava-core 是零依赖的核心模块");
        System.out.println("要运行完整应用，需要添加:");
        System.out.println("  - litejava-plugins (JSON、服务器、数据库等)");
        System.out.println("  - 或自己实现 ServerPlugin、JsonPlugin");
    }
}
