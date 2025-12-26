package litejava.plugins.security;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

/**
 * CORS 跨域中间件 - 处理跨域请求
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>{@code
 * cors:
 *   origin: "*"
 *   methods: "GET, POST, PUT, DELETE, PATCH, OPTIONS"
 *   headers: "Content-Type, Authorization"
 *   maxAge: 86400
 *   credentials: false
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用配置文件参数
 * app.use(new CorsPlugin());
 * 
 * // 或指定来源
 * app.use(new CorsPlugin("https://example.com"));
 * }</pre>
 */
public class CorsPlugin extends MiddlewarePlugin {
    
    /** 默认实例（单例访问） */
    public static CorsPlugin instance;
    
    // 默认配置
    public String origin = "*";
    public String methods = "GET, POST, PUT, DELETE, PATCH, OPTIONS";
    public String headers = "Content-Type, Authorization";
    public int maxAge = 86400;
    public boolean credentials = false;
    
    public CorsPlugin() {}
    
    public CorsPlugin(String origin) {
        this.origin = origin;
    }
    
    @Override
    public void config() {
        origin = app.conf.getString("cors", "origin", origin);
        methods = app.conf.getString("cors", "methods", methods);
        headers = app.conf.getString("cors", "headers", headers);
        maxAge = app.conf.getInt("cors", "maxAge", maxAge);
        credentials = app.conf.getBool("cors", "credentials", credentials);
        
        if (instance == null) instance = this;
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        ctx.header("Access-Control-Allow-Origin", origin);
        ctx.header("Access-Control-Allow-Methods", methods);
        ctx.header("Access-Control-Allow-Headers", headers);
        ctx.header("Access-Control-Max-Age", String.valueOf(maxAge));
        
        if (credentials) {
            ctx.header("Access-Control-Allow-Credentials", "true");
        }
        
        if ("OPTIONS".equals(ctx.method)) {
            ctx.status(204);
            return;
        }
        
        next.run();
    }
}
