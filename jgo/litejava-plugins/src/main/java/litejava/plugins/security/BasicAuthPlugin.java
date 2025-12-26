package litejava.plugins.security;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Basic Auth 中间件 - HTTP 基础认证
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * basicAuth.realm=Restricted
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 简单用户名密码
 * app.use(new BasicAuthPlugin("admin", "secret"));
 * 
 * // 自定义验证逻辑
 * app.use(new BasicAuthPlugin((user, pass) -> {
 *     return userService.verify(user, pass);
 * }));
 * 
 * // 指定 realm
 * BasicAuthPlugin auth = new BasicAuthPlugin("admin", "secret");
 * auth.realm = "Admin Area";
 * app.use(auth);
 * }</pre>
 */
public class BasicAuthPlugin extends MiddlewarePlugin {
    
    private final BiPredicate<String, String> validator;
    public String realm = "Restricted";
    
    public BasicAuthPlugin(String username, String password) {
        this.validator = (u, p) -> username.equals(u) && password.equals(p);
    }
    
    public BasicAuthPlugin(BiPredicate<String, String> validator) {
        this.validator = validator;
    }
    
    @Override
    public void config() {
        realm = app.conf.getString("basicAuth", "realm", realm);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String auth = ctx.headers.get("Authorization");
        
        if (auth != null && auth.startsWith("Basic ")) {
            String encoded = auth.substring(6);
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            
            if (parts.length == 2 && validator.test(parts[0], parts[1])) {
                ctx.state.put("user", parts[0]);
                next.run();
                return;
            }
        }
        
        ctx.status(401)
           .header("WWW-Authenticate", "Basic realm=\"" + realm + "\"")
           .json(Map.of("error", "Unauthorized"));
    }
}
