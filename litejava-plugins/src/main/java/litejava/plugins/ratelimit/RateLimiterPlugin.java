package litejava.plugins.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流插件 - 基于 Resilience4j RateLimiter
 * 
 * 支持：
 * 1. 全局限流 - 所有请求共享限流
 * 2. 路径限流 - 按 path 独立限流
 * 3. 用户限流 - 按 userId 独立限流
 * 
 * 配置示例：
 * ratelimit:
 *   enabled: true
 *   mode: path          # global / path / user
 *   limitForPeriod: 100 # 周期内允许的请求数
 *   limitRefreshPeriod: 1000  # 刷新周期(ms)
 *   timeoutDuration: 500      # 等待超时(ms)
 */
public class RateLimiterPlugin extends MiddlewarePlugin {
    
    public boolean enabled = true;
    public String mode = "path";  // global / path / user
    public int limitForPeriod = 100;
    public int limitRefreshPeriod = 1000;  // ms
    public int timeoutDuration = 500;      // ms
    
    private RateLimiterRegistry registry;
    private RateLimiter globalLimiter;
    private Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public void config() {
        enabled = app.conf.getBool("ratelimit", "enabled", enabled);
        mode = app.conf.getString("ratelimit", "mode", mode);
        limitForPeriod = app.conf.getInt("ratelimit", "limitForPeriod", limitForPeriod);
        limitRefreshPeriod = app.conf.getInt("ratelimit", "limitRefreshPeriod", limitRefreshPeriod);
        timeoutDuration = app.conf.getInt("ratelimit", "timeoutDuration", timeoutDuration);
        
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(limitForPeriod)
            .limitRefreshPeriod(Duration.ofMillis(limitRefreshPeriod))
            .timeoutDuration(Duration.ofMillis(timeoutDuration))
            .build();
        
        registry = RateLimiterRegistry.of(config);
        globalLimiter = registry.rateLimiter("global");
        
        app.log.info("[RateLimiter] 已启用，模式: " + mode + ", 限制: " + limitForPeriod + "/" + limitRefreshPeriod + "ms");
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        if (!enabled) {
            next.run();
            return;
        }
        
        RateLimiter limiter = getLimiter(ctx);
        
        if (limiter.acquirePermission()) {
            next.run();
        } else {
            ctx.status(429).fail(429, "请求过于频繁，请稍后重试");
        }
    }
    
    private RateLimiter getLimiter(Context ctx) {
        switch (mode) {
            case "user":
                Object userId = ctx.state.get("userId");
                if (userId != null) {
                    return limiters.computeIfAbsent("user:" + userId, 
                        k -> registry.rateLimiter(k));
                }
                return globalLimiter;
            case "path":
                return limiters.computeIfAbsent("path:" + ctx.path, 
                    k -> registry.rateLimiter(k));
            default:
                return globalLimiter;
        }
    }
    
    /**
     * 获取限流器状态
     */
    public Map<String, Object> getStats(String name) {
        RateLimiter limiter = limiters.get(name);
        if (limiter == null) limiter = globalLimiter;
        
        RateLimiter.Metrics metrics = limiter.getMetrics();
        return Map.of(
            "availablePermissions", metrics.getAvailablePermissions(),
            "numberOfWaitingThreads", metrics.getNumberOfWaitingThreads()
        );
    }
}
