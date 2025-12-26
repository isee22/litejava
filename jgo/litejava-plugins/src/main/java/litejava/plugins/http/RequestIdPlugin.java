package litejava.plugins.http;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.UUID;

/**
 * 请求ID中间件 - 为每个请求生成唯一ID，用于链路追踪
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * requestId.headerName=X-Request-Id
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new RequestIdPlugin());
 * 
 * // 在 Handler 中获取请求ID
 * app.get("/api/test", ctx -> {
 *     String requestId = RequestIdPlugin.getId(ctx);
 *     ctx.ok(Map.of("requestId", requestId));
 * });
 * 
 * // 响应头中会自动添加 X-Request-Id
 * }</pre>
 */
public class RequestIdPlugin extends MiddlewarePlugin {
    
    public String headerName = "X-Request-Id";
    public String stateKey = "requestId";
    
    @Override
    public void config() {
        headerName = app.conf.getString("requestId", "headerName", headerName);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        // 优先使用客户端传入的请求ID
        String requestId = ctx.headers.get(headerName);
        if (requestId == null || requestId.isEmpty()) {
            requestId = generateId();
        }
        
        ctx.state.put(stateKey, requestId);
        ctx.header(headerName, requestId);
        
        next.run();
    }
    
    protected String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    public static String getId(Context ctx) {
        return (String) ctx.state.get("requestId");
    }
}
