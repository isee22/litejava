package example;

import litejava.plugins.config.YamlConfPlugin;
import litejava.plugins.json.JacksonPlugin;
import litejava.plugins.server.JettyServerPlugin;

import java.util.Map;

/**
 * 自定义 App 使用示例
 * 
 * <p>演示如何使用继承自 App 的 MyApp 类。
 */
public class CustomAppExample {
    
    public static void main(String[] args) {
        // 使用自定义 App
        MyApp app = new MyApp();
        
        // 注册插件
        app.use(new YamlConfPlugin());
        app.use(new JacksonPlugin());
        app.use(new JettyServerPlugin());
        
        // 正常路由 - 使用统一响应格式
        app.get("/", ctx -> MyApp.ok(ctx, Map.of("message", "Hello from MyApp!")));
        
        app.get("/users", ctx -> {
            java.util.List<Map<String, Object>> users = java.util.Arrays.asList(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob")
            );
            MyApp.ok(ctx, users);
        });
        
        app.get("/users/:id", ctx -> {
            long id = ctx.pathParamLong("id");
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid user id");
            }
            MyApp.ok(ctx, Map.of("id", id, "name", "User" + id));
        });
        
        // 测试错误处理
        app.get("/error/400", ctx -> {
            throw new IllegalArgumentException("Bad request parameter");
        });
        
        app.get("/error/403", ctx -> {
            throw new SecurityException("Access denied");
        });
        
        app.get("/error/500", ctx -> {
            throw new RuntimeException("Something went wrong");
        });
        
        app.run();
    }
}
