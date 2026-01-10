package gateway.filter;

import gateway.G;
import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;
import litejava.plugins.http.RpcClient;

import java.util.*;

/**
 * K8s 代理过滤器
 * 
 * 使用 K8s Service DNS 做服务发现：
 * - /product/* → http://product-service:8083/*
 * - /user/*    → http://user-service:8081/*
 * - /order/*   → http://order-service:8082/*
 * 
 * K8s Service 会自动负载均衡到后端 Pod
 */
public class K8sProxyFilter extends MiddlewarePlugin {
    
    // 路由表: 路径前缀 → K8s Service URL
    private final Map<String, String> routeTable = new LinkedHashMap<>();
    
    @Override
    public void config() {
        // 从配置文件加载路由表
        loadRoutes();
    }
    
    @SuppressWarnings("unchecked")
    private void loadRoutes() {
        // 默认路由配置 (可通过 ConfigMap 覆盖)
        Map<String, Object> gateway = (Map<String, Object>) G.app.conf.get().get("gateway");
        Map<String, Object> routes = gateway != null ? (Map<String, Object>) gateway.get("routes") : null;
        
        if (routes != null && !routes.isEmpty()) {
            for (Map.Entry<String, Object> entry : routes.entrySet()) {
                String prefix = entry.getKey();
                String target = String.valueOf(entry.getValue());
                routeTable.put(prefix, target);
                G.app.log.info("[K8s Gateway] 路由: " + prefix + " -> " + target);
            }
        } else {
            // 默认路由 (服务名即 K8s Service 名)
            routeTable.put("/product", "http://product-service:8083");
            routeTable.put("/user", "http://user-service:8081");
            routeTable.put("/order", "http://order-service:8082");
            routeTable.put("/auth", "http://auth-service:8085");
            routeTable.put("/payment", "http://payment-service:8084");
            G.app.log.info("[K8s Gateway] 使用默认路由表");
        }
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String path = ctx.path;
        
        // 跳过健康检查
        if ("/health".equals(path) || path.startsWith("/health/")) {
            next.run();
            return;
        }
        
        // 查找匹配的路由
        String targetBase = findTarget(path);
        if (targetBase != null) {
            proxyRequest(ctx, targetBase, path);
            return;
        }
        
        next.run();
    }
    
    private String findTarget(String path) {
        for (Map.Entry<String, String> entry : routeTable.entrySet()) {
            String prefix = entry.getKey();
            if (path.startsWith(prefix + "/") || path.equals(prefix)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    private void proxyRequest(Context ctx, String targetBase, String path) {
        String targetUrl = targetBase + path;
        if (ctx.query != null && !ctx.query.isEmpty()) {
            targetUrl += "?" + ctx.query;
        }
        
        String finalUrl = targetUrl;
        try {
            String response = G.circuitBreaker.execute(
                targetBase,
                () -> doProxy(ctx, finalUrl),
                () -> "{\"code\":503,\"msg\":\"服务暂时不可用\"}"
            );
            ctx.header("Content-Type", "application/json");
            ctx.text(response);
        } catch (Exception e) {
            ctx.status(502).fail(502, "网关错误: " + e.getMessage());
        }
    }
    
    private String doProxy(Context ctx, String targetUrl) {
        try {
            RpcClient rpc = G.app.getPlugin(RpcClient.class);
            
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", ctx.header("Content-Type"));
            headers.put("Accept", "application/json");
            
            // 传递用户信息
            Object userId = ctx.state.get("userId");
            if (userId != null) {
                headers.put("X-User-Id", String.valueOf(userId));
                headers.put("X-Username", (String) ctx.state.get("username"));
            }
            
            // 传递链路追踪
            Object traceId = ctx.state.get("traceId");
            Object spanId = ctx.state.get("spanId");
            if (traceId != null) {
                headers.put("X-Trace-Id", traceId.toString());
            }
            if (spanId != null) {
                headers.put("X-Span-Id", spanId.toString());
            }
            
            byte[] body = null;
            if ("POST".equals(ctx.method) || "PUT".equals(ctx.method) || "PATCH".equals(ctx.method)) {
                body = ctx.getRawData();
            }
            
            RpcClient.ProxyResponse resp = rpc.proxy(ctx.method, targetUrl, body, headers);
            ctx.status(resp.code);
            return resp.body != null ? resp.body : "{}";
        } catch (Exception e) {
            throw new RuntimeException("代理请求失败: " + e.getMessage(), e);
        }
    }
}
