package litejava.plugins.security;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * CSRF 保护中间件 - 防止跨站请求伪造
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * csrf.headerName=X-CSRF-Token
 * csrf.paramName=_csrf
 * csrf.sessionKey=_csrfToken
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new SessionPlugin());  // 需要先启用 Session
 * app.use(new CsrfPlugin());
 * 
 * // 在表单中添加 token
 * // <input type="hidden" name="_csrf" value="${csrfToken}">
 * 
 * // 或在 AJAX 请求头中
 * // X-CSRF-Token: ${csrfToken}
 * 
 * // 获取 token
 * String token = CsrfPlugin.getToken(ctx);
 * }</pre>
 */
public class CsrfPlugin extends MiddlewarePlugin {
    
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    public String headerName = "X-CSRF-Token";
    public String paramName = "_csrf";
    public String sessionKey = "_csrfToken";
    
    @Override
    public void config() {
        headerName = app.conf.getString("csrf", "headerName", headerName);
        paramName = app.conf.getString("csrf", "paramName", paramName);
        sessionKey = app.conf.getString("csrf", "sessionKey", sessionKey);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> session = (Map<String, Object>) ctx.state.get("session");
        
        if (session == null) {
            throw new IllegalStateException("CsrfPlugin requires SessionPlugin");
        }
        
        // 生成或获取 token
        String token = (String) session.get(sessionKey);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.put(sessionKey, token);
        }
        ctx.state.put("csrfToken", token);
        
        // 安全方法不检查
        if (SAFE_METHODS.contains(ctx.method)) {
            next.run();
            return;
        }
        
        // 验证 token
        String requestToken = ctx.headers.get(headerName);
        if (requestToken == null) {
            Map<String, Object> body = ctx.bindJSON();
            requestToken = (String) body.get(paramName);
        }
        
        if (!token.equals(requestToken)) {
            ctx.status(403).json(Map.of("error", "Invalid CSRF token"));
            return;
        }
        
        next.run();
    }
    
    public static String getToken(Context ctx) {
        return (String) ctx.state.get("csrfToken");
    }
}
