package litejava.plugins.health;

import litejava.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查插件
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new HealthPlugin());          // GET /health
 * app.use(new HealthPlugin("/status")); // GET /status
 * }</pre>
 * 
 * <h2>响应示例</h2>
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "env": "dev",
 *   "timestamp": 1703404800000
 * }
 * }</pre>
 */
public class HealthPlugin extends Plugin {
    
    public String path = "/health";
    
    public HealthPlugin() {}
    
    public HealthPlugin(String path) {
        this.path = path;
    }
    
    @Override
    public void config() {
        path = app.conf.getString("health", "path", path);
        
        app.get(path, ctx -> {
            Map<String, Object> health = new LinkedHashMap<>();
            health.put("status", "UP");
            health.put("env", app.env);
            health.put("timestamp", System.currentTimeMillis());
            ctx.ok(health);
        });
    }
}
