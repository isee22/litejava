package gateway.filter;

import gateway.G;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AuthFilter extends MiddlewarePlugin {
    
    public SecretKey jwtKey;
    public Set<String> publicPaths = new HashSet<>();
    
    @Override
    @SuppressWarnings("unchecked")
    public void config() {
        String secret = G.app.conf.getString("jwt", "secret", "default-secret-key-must-be-32-chars");
        jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        // 从配置文件获取公开路径
        Object pathsObj = G.app.conf.get().get("publicPaths");
        if (pathsObj instanceof List) {
            List<?> paths = (List<?>) pathsObj;
            for (Object p : paths) {
                if (p != null) {
                    publicPaths.add(p.toString());
                }
            }
        }
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        // 健康检查等路径直接放行
        if (ctx.path.equals("/health") || ctx.path.startsWith("/health/")) {
            next.run();
            return;
        }
        
        // 公开路径放行
        if (isPublicPath(ctx.path)) {
            next.run();
            return;
        }
        
        String token = ctx.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            ctx.status(401).fail(401, "未登录，请先登录");
            return;
        }
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            ctx.state.put("userId", Long.parseLong(claims.getSubject()));
            ctx.state.put("username", claims.get("username", String.class));
            next.run();
        } catch (ExpiredJwtException e) {
            ctx.status(401).fail(401, "登录已过期，请重新登录");
        } catch (Exception e) {
            ctx.status(401).fail(401, "Token 无效");
        }
    }
    
    private boolean isPublicPath(String path) {
        for (String publicPath : publicPaths) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) {
                return true;
            }
        }
        return false;
    }
}
