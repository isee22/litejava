package litejava.plugins.security;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理中间件 - 基于 Cookie 的会话
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>{@code
 * session:
 *   cookieName: JSESSIONID
 *   maxAge: 3600       # 秒
 *   httpOnly: true
 *   secure: false
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用配置文件参数
 * app.use(new SessionPlugin());
 * 
 * // 在 Handler 中
 * Map<String, Object> session = SessionPlugin.get(ctx);
 * session.put("userId", 123);
 * }</pre>
 */
public class SessionPlugin extends MiddlewarePlugin {
    
    public final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    
    // 默认配置
    public String cookieName = "JSESSIONID";
    public int maxAge = 3600;
    public boolean httpOnly = true;
    public boolean secure = false;
    
    @Override
    public void config() {
        cookieName = app.conf.getString("session", "cookieName", cookieName);
        maxAge = app.conf.getInt("session", "maxAge", maxAge);
        httpOnly = app.conf.getBool("session", "httpOnly", httpOnly);
        secure = app.conf.getBool("session", "secure", secure);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String sessionId = getSessionId(ctx);
        
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, new ConcurrentHashMap<>());
            
            StringBuilder cookie = new StringBuilder();
            cookie.append(cookieName).append("=").append(sessionId);
            cookie.append("; Path=/");
            cookie.append("; Max-Age=").append(maxAge);
            if (httpOnly) cookie.append("; HttpOnly");
            if (secure) cookie.append("; Secure");
            
            ctx.header("Set-Cookie", cookie.toString());
        }
        
        ctx.state.put("sessionId", sessionId);
        ctx.state.put("session", sessions.get(sessionId));
        
        next.run();
    }
    
    private String getSessionId(Context ctx) {
        String cookie = ctx.headers.get("Cookie");
        if (cookie == null) return null;
        
        for (String part : cookie.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(cookieName)) {
                return kv[1];
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> get(Context ctx) {
        return (Map<String, Object>) ctx.state.get("session");
    }
}
