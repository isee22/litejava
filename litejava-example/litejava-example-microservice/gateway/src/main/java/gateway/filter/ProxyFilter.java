package gateway.filter;

import gateway.G;
import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;
import litejava.plugins.gateway.GrayReleaseFilter;
import litejava.plugins.http.RpcClient;
import litejava.plugins.microservice.DiscoveryPlugin.ServiceInstance;

import java.util.*;
import java.util.concurrent.*;

/**
 * 代理过滤器 - 透明代理模式
 * 
 * 前端无感调用，就像单体应用一样：
 * - /product/list → 自动路由到 product-service
 * - /user/info → 自动路由到 user-service
 * - /order/create → 自动路由到 order-service
 * 
 * 路由发现机制：
 * 1. 从 Consul 获取所有服务的元数据（包含路由前缀）
 * 2. 建立 路径前缀 → 服务 的映射表
 * 3. 定时刷新路由表
 */
public class ProxyFilter extends MiddlewarePlugin {
    
    // 路径前缀 → 服务名 映射 (如 /product → product-service)
    private final ConcurrentHashMap<String, String> routeTable = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    
    @Override
    public void config() {
        // 初始化路由表
        refreshRouteTable();
        
        // 定时刷新（每30秒）
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "route-refresh");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::refreshRouteTable, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 从 Consul 刷新路由表
     */
    private void refreshRouteTable() {
        try {
            Map<String, List<ServiceInstance>> services = G.consul().getAllServices();
            Map<String, String> newRoutes = new HashMap<>();
            
            for (Map.Entry<String, List<ServiceInstance>> entry : services.entrySet()) {
                String serviceName = entry.getKey();
                List<ServiceInstance> instances = entry.getValue();
                
                if (instances.isEmpty()) continue;
                
                // 从服务元数据获取路由前缀，或根据服务名推断
                ServiceInstance instance = instances.get(0);
                String routePrefix = getRoutePrefix(serviceName, instance);
                
                if (routePrefix != null) {
                    newRoutes.put(routePrefix, serviceName);
                }
            }
            
            // 更新路由表
            routeTable.clear();
            routeTable.putAll(newRoutes);
            
            if (!newRoutes.isEmpty()) {
                G.app.log.debug("[Gateway] 路由表已刷新: " + newRoutes);
            }
        } catch (Exception e) {
            G.app.log.warn("[Gateway] 刷新路由表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取服务的路由前缀
     * 优先从元数据读取，否则根据服务名推断
     */
    private String getRoutePrefix(String serviceName, ServiceInstance instance) {
        // 1. 从元数据获取 (服务可配置 route.prefix=/product)
        Map<String, String> meta = instance.getMeta();
        if (meta != null && meta.containsKey("routePrefix")) {
            return meta.get("routePrefix");
        }
        
        // 2. 根据服务名推断: xxx-service → /xxx
        if (serviceName.endsWith("-service")) {
            return "/" + serviceName.substring(0, serviceName.length() - 8);
        }
        
        // 3. 网关自身不代理
        if ("gateway".equals(serviceName)) {
            return null;
        }
        
        return "/" + serviceName;
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String path = ctx.path;
        
        // 跳过健康检查等
        if ("/health".equals(path) || path.startsWith("/health/")) {
            next.run();
            return;
        }
        
        // 透明路由：根据路径前缀找服务
        String serviceName = findService(path);
        if (serviceName != null) {
            ServiceInstance instance = selectInstance(ctx, serviceName);
            if (instance != null) {
                proxyDirect(ctx, serviceName, instance, path);
                return;
            }
            ctx.status(503).fail(503, "服务不可用: " + serviceName);
            return;
        }
        
        next.run();
    }
    
    /**
     * 根据路径找到对应的服务
     * /product/list → product-service
     * /user/info → user-service
     */
    private String findService(String path) {
        // 精确匹配路径前缀
        for (Map.Entry<String, String> entry : routeTable.entrySet()) {
            String prefix = entry.getKey();
            if (path.startsWith(prefix + "/") || path.equals(prefix)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * 选择服务实例（支持灰度）
     */
    private ServiceInstance selectInstance(Context ctx, String serviceName) {
        // 检查是否有灰度标记
        String grayVersion = (String) ctx.state.get("grayVersion");
        String grayService = (String) ctx.state.get("grayService");
        
        // 如果是灰度服务，尝试用 GrayReleaseFilter 选择实例
        if (serviceName.equals(grayService) && grayVersion != null) {
            GrayReleaseFilter grayFilter = G.app.getPlugin(GrayReleaseFilter.class);
            if (grayFilter != null) {
                ServiceInstance grayInstance = grayFilter.selectInstance(ctx, serviceName);
                if (grayInstance != null) {
                    G.app.log.info("[Gray] 路由到灰度实例: " + serviceName + " -> " + grayVersion);
                    return grayInstance;
                }
            }
        }
        
        // 默认：从 Consul 获取实例
        return G.consul().getInstance(serviceName);
    }
    
    private void proxyDirect(Context ctx, String serviceName, ServiceInstance instance, String targetPath) {
        doProxyRequest(ctx, serviceName, instance, targetPath);
    }
    
    private void doProxyRequest(Context ctx, String serviceName, ServiceInstance instance, String targetPath) {
        String targetUrl = instance.getUrl() + targetPath;
        if (ctx.query != null && !ctx.query.isEmpty()) {
            targetUrl += "?" + ctx.query;
        }
        
        String finalUrl = targetUrl;
        try {
            String response = G.circuitBreaker.execute(
                serviceName,
                () -> doProxy(ctx, finalUrl),
                () -> "{\"code\":503,\"msg\":\"服务暂时不可用，请稍后重试\"}"
            );
            ctx.header("Content-Type", "application/json");
            ctx.text(response);
        } catch (Exception e) {
            ctx.status(502).fail(502, "网关错误：" + e.getMessage());
        }
    }
    
    private String doProxy(Context ctx, String targetUrl) {
        try {
            RpcClient rpc = G.app.getPlugin(RpcClient.class);
            
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", ctx.header("Content-Type"));
            headers.put("Accept", "application/json");
            
            Object userId = ctx.state.get("userId");
            if (userId != null) {
                headers.put("X-User-Id", String.valueOf(userId));
                headers.put("X-Username", (String) ctx.state.get("username"));
            }
            
            Object traceId = ctx.state.get("traceId");
            Object spanId = ctx.state.get("spanId");
            if (traceId != null) {
                headers.put("X-Trace-Id", traceId.toString());
            }
            if (spanId != null) {
                headers.put("X-Span-Id", spanId.toString());
            }
            
            Object seataXid = ctx.state.get("seataXid");
            String reqXid = ctx.header("X-Seata-Xid");
            if (seataXid != null) {
                headers.put("X-Seata-Xid", seataXid.toString());
            } else if (reqXid != null && !reqXid.isEmpty()) {
                headers.put("X-Seata-Xid", reqXid);
            }
            
            byte[] body = null;
            if ("POST".equals(ctx.method) || "PUT".equals(ctx.method) || "PATCH".equals(ctx.method)) {
                body = ctx.getRawData();
            }
            
            RpcClient.ProxyResponse resp = rpc.proxy(ctx.method, targetUrl, body, headers);
            ctx.status(resp.code);
            return resp.body != null ? resp.body : "{}";
        } catch (Exception e) {
            throw new RuntimeException("代理请求失败：" + e.getMessage(), e);
        }
    }
    
    @Override
    public void uninstall() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
