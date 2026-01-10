package gateway;

import gateway.filter.AuthFilter;
import gateway.filter.ProxyFilter;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.gateway.GrayReleaseFilter;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.http.RpcClient;
import litejava.plugins.log.LogstashPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import litejava.plugins.ratelimit.RateLimiterPlugin;
import litejava.plugins.security.CorsPlugin;
import litejava.plugins.tracing.TracingPlugin;

/**
 * API 网关启动类
 */
public class GatewayApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        // 统一异常处理（必须在最前面）
        app.use(new RecoveryPlugin());
        
        // 基础设施插件
        app.use(new ConsulPlugin());
        app.use(new RpcClient());           // HTTP 客户端
        app.use(new CorsPlugin());
        app.use(new TracingPlugin());       // 链路追踪
        app.use(new RateLimiterPlugin());   // 限流
        app.use(new LogstashPlugin());      // ELK 日志聚合
        
        // 灰度发布
        GrayReleaseFilter grayFilter = new GrayReleaseFilter();
        grayFilter.discovery(app.getPlugin(ConsulPlugin.class));
        app.use(grayFilter);
        
        HealthCheck health = new HealthCheck();
        health.addCheck("self", () -> true);
        app.use(health);
        
        G.init();
        
        // 业务中间件
        app.use(new AuthFilter());
        app.use(new ProxyFilter());
        
        // 透明代理：捕获所有 API 请求
        // ProxyFilter 会根据路径前缀自动路由到对应服务
        app.any("/*path", ctx -> {
            // 由 ProxyFilter 中间件处理
        });
        
        app.run(app.conf.getInt("server", "port", 8080));
    }
}
