package example.infra;

import example.service.AuthService;
import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.Map;

/**
 * 认证中间件
 */
public class AuthMiddleware extends MiddlewarePlugin {
    
    private final AuthService authService = new AuthService();
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String path = ctx.path;
        
        // 白名单
        if (path.equals("/") || 
            path.startsWith("/static") || 
            path.startsWith("/api/books") ||
            path.equals("/api/auth/login") ||
            path.equals("/api/auth/register")) {
            next.run();
            return;
        }
        
        String token = ctx.headers.get("Authorization");
        if (token == null || token.isEmpty()) {
            ctx.status(401).json(Map.of("error", "未登录"));
            return;
        }
        
        String username = authService.validateToken(token.replace("Bearer ", ""));
        if (username == null) {
            ctx.status(401).json(Map.of("error", "登录已过期"));
            return;
        }
        
        ctx.state.put("username", username);
        next.run();
    }
}
