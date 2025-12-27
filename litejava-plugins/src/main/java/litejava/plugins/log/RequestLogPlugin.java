package litejava.plugins.log;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

/**
 * 请求日志中间件 - 记录每个请求的方法、路径、状态码和耗时
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * requestLog.enabled=true
 * requestLog.format=%s %s %d %dms
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new RequestLogPlugin());
 * 
 * // 输出示例：
 * // GET /api/users 200 15ms
 * // POST /api/login 401 8ms
 * }</pre>
 */
public class RequestLogPlugin extends MiddlewarePlugin {
    
    /** 默认实例（单例访问） */
    public static RequestLogPlugin instance;
    
    public boolean enabled = true;
    public String format = "%s %s %d %dms";
    
    public RequestLogPlugin() {
        instance = this;
    }
    
    @Override
    public void config() {
        enabled = app.conf.getBool("requestLog", "enabled", enabled);
        format = app.conf.getString("requestLog", "format", format);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        if (!enabled) {
            next.run();
            return;
        }
        
        long start = System.currentTimeMillis();
        
        next.run();
        
        long duration = System.currentTimeMillis() - start;
        app.log.info(String.format(format, ctx.method, ctx.path, ctx.getResponseStatus(), duration));
    }
}
