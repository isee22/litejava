package litejava.plugins.tracing;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;
import litejava.plugins.http.HttpClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 链路追踪插件 - 支持 Zipkin
 * 
 * 功能：
 * 1. 生成/传递 traceId, spanId
 * 2. 记录请求耗时
 * 3. 异步上报到 Zipkin
 * 
 * 配置：
 * tracing:
 *   enabled: true
 *   endpoint: http://localhost:9411/api/v2/spans
 */
public class TracingPlugin extends MiddlewarePlugin {
    
    public boolean enabled = true;
    public String endpoint = "http://localhost:9411/api/v2/spans";
    public String serviceName = "unknown";
    
    private HttpClient http;
    private ExecutorService executor;
    
    @Override
    public void config() {
        enabled = app.conf.getBool("tracing", "enabled", true);
        endpoint = app.conf.getString("tracing", "endpoint", endpoint);
        serviceName = app.conf.getString("server", "name", serviceName);
        
        if (enabled) {
            http = new HttpClient(app).timeout(3000, 3000);
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "tracing-reporter");
                t.setDaemon(true);
                return t;
            });
            app.log.info("[Tracing] 链路追踪已启用，上报地址: " + endpoint);
        }
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        if (!enabled) {
            next.run();
            return;
        }
        
        // 从请求头获取或生成 traceId
        String traceId = ctx.header("X-Trace-Id");
        String parentSpanId = ctx.header("X-Span-Id");
        
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateId();
        }
        String spanId = generateId();
        
        // 存入 context，供后续使用
        ctx.state.put("traceId", traceId);
        ctx.state.put("spanId", spanId);
        ctx.state.put("parentSpanId", parentSpanId);
        
        // 设置响应头，方便前端调试
        ctx.header("X-Trace-Id", traceId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            next.run();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 异步上报
            String finalTraceId = traceId;
            String finalSpanId = spanId;
            String finalParentSpanId = parentSpanId;
            String name = ctx.method + " " + ctx.path;
            int status = ctx.getResponseStatus();
            
            executor.submit(() -> reportSpan(
                finalTraceId, finalSpanId, finalParentSpanId,
                name, startTime * 1000, duration * 1000, status
            ));
        }
    }
    
    private void reportSpan(String traceId, String spanId, String parentSpanId,
                           String name, long timestamp, long duration, int statusCode) {
        try {
            // Zipkin V2 JSON 格式
            StringBuilder json = new StringBuilder();
            json.append("[{");
            json.append("\"traceId\":\"").append(traceId).append("\",");
            json.append("\"id\":\"").append(spanId).append("\",");
            if (parentSpanId != null && !parentSpanId.isEmpty()) {
                json.append("\"parentId\":\"").append(parentSpanId).append("\",");
            }
            json.append("\"name\":\"").append(name).append("\",");
            json.append("\"timestamp\":").append(timestamp).append(",");
            json.append("\"duration\":").append(duration).append(",");
            json.append("\"localEndpoint\":{\"serviceName\":\"").append(serviceName).append("\"},");
            json.append("\"tags\":{\"http.status_code\":\"").append(statusCode).append("\"}");
            json.append("}]");
            
            http.postJson(endpoint, json.toString());
        } catch (Exception e) {
            // 上报失败不影响业务
        }
    }
    
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    @Override
    public void uninstall() {
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    /**
     * 获取当前请求的 traceId（供日志使用）
     */
    public static String getTraceId(Context ctx) {
        Object traceId = ctx.state.get("traceId");
        return traceId != null ? traceId.toString() : "-";
    }
    
    /**
     * 获取当前请求的 spanId
     */
    public static String getSpanId(Context ctx) {
        Object spanId = ctx.state.get("spanId");
        return spanId != null ? spanId.toString() : "-";
    }
}
