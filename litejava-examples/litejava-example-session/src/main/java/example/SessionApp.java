package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.cache.RedisPlugin;
import litejava.plugins.cluster.RedisSessionPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 分布式 Session 示例
 * 
 * <h2>前置条件</h2>
 * 启动 Redis: docker run -d -p 6379:6379 redis
 * 
 * <h2>测试</h2>
 * <pre>
 * # 登录（创建 session）
 * curl -X POST http://localhost:8080/login -H "Content-Type: application/json" \
 *   -d '{"username":"alice"}'
 * 
 * # 获取 session
 * curl http://localhost:8080/session -H "X-Session-Id: {sessionId}"
 * 
 * # 登出
 * curl -X POST http://localhost:8080/logout -H "X-Session-Id: {sessionId}"
 * </pre>
 */
public class SessionApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // Redis 连接
        RedisPlugin redis = new RedisPlugin();
        redis.host = "localhost";
        redis.port = 6379;
        app.use(redis);
        
        // Session 插件
        RedisSessionPlugin session = new RedisSessionPlugin(redis);
        session.ttl = 1800; // 30 minutes
        app.use(session);
        
        // 登录
        app.post("/login", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            String username = (String) body.get("username");
            
            Map<String, String> sessionData = new LinkedHashMap<>();
            sessionData.put("username", username);
            sessionData.put("loginTime", String.valueOf(System.currentTimeMillis()));
            String sessionId = session.createSession(sessionData);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sessionId", sessionId);
            result.put("username", username);
            ctx.json(result);
        });
        
        // 获取 session
        app.get("/session", ctx -> {
            String sessionId = ctx.header("X-Session-Id");
            if (sessionId == null) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "No session");
                ctx.status(401).json(err);
                return;
            }
            
            Map<String, String> data = session.getSession(sessionId);
            if (data.isEmpty()) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "Session expired");
                ctx.status(401).json(err);
                return;
            }
            
            ctx.json(data);
        });
        
        // 更新 session
        app.put("/session", ctx -> {
            String sessionId = ctx.header("X-Session-Id");
            if (sessionId == null) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "No session");
                ctx.status(401).json(err);
                return;
            }
            
            Map<String, Object> body = ctx.bindJSON();
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                session.setSession(sessionId, entry.getKey(), String.valueOf(entry.getValue()));
            }
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("updated", true);
            ctx.json(result);
        });
        
        // 登出
        app.post("/logout", ctx -> {
            String sessionId = ctx.header("X-Session-Id");
            if (sessionId != null) {
                session.destroySession(sessionId);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("loggedOut", true);
            ctx.json(result);
        });
        
        // 首页
        app.get("/", ctx -> {
            Map<String, Object> endpoints = new LinkedHashMap<>();
            endpoints.put("POST /login", "Create session");
            endpoints.put("GET /session", "Get session data");
            endpoints.put("PUT /session", "Update session");
            endpoints.put("POST /logout", "Destroy session");
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Redis Session Example");
            result.put("endpoints", endpoints);
            ctx.json(result);
        });
        
        app.run();
    }
}
