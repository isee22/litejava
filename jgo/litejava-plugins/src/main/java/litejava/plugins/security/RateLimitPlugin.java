package litejava.plugins.security;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流中间件 - 基于 IP 的请求限流
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>{@code
 * rateLimit:
 *   maxRequests: 100   # 最大请求数
 *   windowMs: 60000    # 时间窗口（毫秒）
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用配置文件参数
 * app.use(new RateLimitPlugin());
 * 
 * // 或手动设置：每分钟10次
 * app.use(new RateLimitPlugin(10, 60000));
 * }</pre>
 */
public class RateLimitPlugin extends MiddlewarePlugin {
    
    public final Map<String, long[]> requests = new ConcurrentHashMap<>();
    
    // 默认配置
    public int maxRequests = 100;
    public int windowMs = 60000;
    
    public RateLimitPlugin() {}
    
    public RateLimitPlugin(int maxRequests, int windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }
    
    @Override
    public void config() {
        maxRequests = app.conf.getInt("rateLimit", "maxRequests", maxRequests);
        windowMs = app.conf.getInt("rateLimit", "windowMs", windowMs);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String ip = ctx.clientIP();
        if (ip == null) ip = "unknown";
        
        long now = System.currentTimeMillis();
        
        long[] record = requests.computeIfAbsent(ip, k -> new long[]{0, now});
        
        synchronized (record) {
            if (now - record[1] > windowMs) {
                record[0] = 0;
                record[1] = now;
            }
            
            record[0]++;
            
            if (record[0] > maxRequests) {
                ctx.status(429)
                   .header("Retry-After", String.valueOf(windowMs / 1000))
                   .json(Map.of("error", "Too Many Requests"));
                return;
            }
        }
        
        next.run();
    }
}
