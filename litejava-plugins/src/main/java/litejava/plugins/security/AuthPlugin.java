package litejava.plugins.security;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 通用认证插件 - 白名单 + 自定义认证逻辑
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>{@code
 * auth:
 *   whitelist: /,/login,/register,/static*,/public*
 *   headerName: Authorization
 *   tokenPrefix: Bearer 
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式1: 使用 JwtPlugin 验证
 * AuthPlugin auth = new AuthPlugin(token -> {
 *     return JwtPlugin.instance.verify(token);  // 返回 claims Map 或 null
 * });
 * app.use(auth);
 * 
 * // 方式2: 自定义验证逻辑
 * AuthPlugin auth = new AuthPlugin(token -> {
 *     User user = userService.findByToken(token);
 *     if (user == null) return null;
 *     return Map.of("userId", user.id, "username", user.name);
 * });
 * app.use(auth);
 * 
 * // 方式3: 代码配置白名单
 * AuthPlugin auth = new AuthPlugin(this::validateToken)
 *     .whitelist("/", "/login", "/register")
 *     .whitelistPrefix("/static", "/public");
 * app.use(auth);
 * 
 * // 在 Handler 中获取认证信息
 * app.get("/api/me", ctx -> {
 *     Map<String, Object> user = (Map) ctx.state.get("auth");
 *     ctx.ok(user);
 * });
 * }</pre>
 */
public class AuthPlugin extends MiddlewarePlugin {
    
    /** 默认实例 */
    public static AuthPlugin instance;
    
    /** 认证函数：token -> 用户信息 Map（null 表示认证失败） */
    public Function<String, Map<String, Object>> authenticator;
    
    /** 白名单路径（精确匹配） */
    public Set<String> whitelistPaths = new HashSet<>();
    
    /** 白名单前缀（前缀匹配） */
    public Set<String> whitelistPrefixes = new HashSet<>();
    
    /** 认证头名称，默认 Authorization */
    public String headerName = "Authorization";
    
    /** Token 前缀，默认 "Bearer "，会自动去除 */
    public String tokenPrefix = "Bearer ";
    
    /** 认证信息存储到 ctx.state 的 key，默认 "auth" */
    public String stateKey = "auth";
    
    /** 未认证时的错误消息 */
    public String unauthorizedMessage = "Unauthorized";
    
    /** 未认证时的 HTTP 状态码 */
    public int unauthorizedStatus = 401;
    
    public AuthPlugin() {
        instance = this;
    }
    
    public AuthPlugin(Function<String, Map<String, Object>> authenticator) {
        instance = this;
        this.authenticator = authenticator;
    }
    
    /**
     * 添加白名单路径（精确匹配）
     */
    public AuthPlugin whitelist(String... paths) {
        for (String path : paths) {
            whitelistPaths.add(path);
        }
        return this;
    }
    
    /**
     * 添加白名单前缀（前缀匹配）
     */
    public AuthPlugin whitelistPrefix(String... prefixes) {
        for (String prefix : prefixes) {
            whitelistPrefixes.add(prefix);
        }
        return this;
    }
    
    @Override
    public void config() {
        // 从配置文件读取白名单
        String configWhitelist = app.conf.getString("auth", "whitelist", null);
        if (configWhitelist != null && !configWhitelist.isEmpty()) {
            for (String path : configWhitelist.split(",")) {
                path = path.trim();
                if (path.endsWith("*")) {
                    whitelistPrefixes.add(path.substring(0, path.length() - 1));
                } else {
                    whitelistPaths.add(path);
                }
            }
        }
        
        // 从配置文件读取其他配置
        headerName = app.conf.getString("auth", "headerName", headerName);
        tokenPrefix = app.conf.getString("auth", "tokenPrefix", tokenPrefix);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        // 白名单检查
        if (isWhitelisted(ctx.path)) {
            next.run();
            return;
        }
        
        // 获取 token
        String header = ctx.header(headerName);
        if (header == null || header.isEmpty()) {
            unauthorized(ctx);
            return;
        }
        
        // 去除前缀
        String token = header;
        if (tokenPrefix != null && !tokenPrefix.isEmpty() && header.startsWith(tokenPrefix)) {
            token = header.substring(tokenPrefix.length());
        }
        
        // 认证
        if (authenticator == null) {
            // 没有设置认证函数，只检查 token 是否存在
            ctx.state.put(stateKey, Map.of("token", token));
            next.run();
            return;
        }
        
        Map<String, Object> authInfo = authenticator.apply(token);
        if (authInfo == null) {
            unauthorized(ctx);
            return;
        }
        
        // 存储认证信息
        ctx.state.put(stateKey, authInfo);
        next.run();
    }
    
    private boolean isWhitelisted(String path) {
        if (whitelistPaths.contains(path)) {
            return true;
        }
        for (String prefix : whitelistPrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    private void unauthorized(Context ctx) {
        ctx.status(unauthorizedStatus).json(Map.of("error", unauthorizedMessage));
    }
}
