package gateway;

import gateway.filter.AuthFilter;
import gateway.filter.K8sProxyFilter;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.http.RpcClient;
import litejava.plugins.microservice.HealthCheck;
import litejava.plugins.ratelimit.RateLimiterPlugin;
import litejava.plugins.security.CorsPlugin;
import litejava.plugins.tracing.TracingPlugin;

/**
 * K8s 版 API 网关
 * 
 * 与 Consul 版的区别：
 * - 不依赖 Consul，使用 K8s Service DNS 做服务发现
 * - 服务地址格式: http://{service-name}:{port}
 * - K8s Service 自动负载均衡到多个 Pod
 */
public class GatewayK8sApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        // 统一异常处理
        app.use(new RecoveryPlugin());
        
        // 基础插件
        app.use(new RpcClient());
        app.use(new CorsPlugin());
        app.use(new TracingPlugin());
        app.use(new RateLimiterPlugin());
        
        // 健康检查 (K8s liveness/readiness probe)
        HealthCheck health = new HealthCheck();
        health.addCheck("self", () -> true);
        app.use(health);
        
        G.init();
        
        // 业务中间件
        app.use(new AuthFilter());
        app.use(new K8sProxyFilter());
        
        // 透明代理
        app.any("/*path", ctx -> {
            // 由 K8sProxyFilter 处理
        });
        
        int port = app.conf.getInt("server", "port", 8080);
        app.run(port);
    }
}
