package litejava.plugins;

import litejava.App;

import java.util.Map;

/**
 * 测试 DebugPlugin
 */
public class DebugPluginTest {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 手动启用 debug（不通过配置）
        app.use(new DebugPlugin());
        
        // 添加一些路由
        app.get("/", ctx -> ctx.json(Map.of("msg", "Hello")));
        app.get("/users", ctx -> ctx.json(Map.of("users", "list")));
        app.post("/users", ctx -> ctx.json(Map.of("created", true)));
        
        app.run();
    }
}
