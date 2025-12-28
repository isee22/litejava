package example.controller;

import example.Services;
import litejava.Context;
import litejava.Routes;

import java.util.Map;

/**
 * 认证控制器
 */
public class AuthController {
    
    public Routes routes() {
        return new Routes()
            .post("/api/auth/login", this::login)
            .post("/api/auth/register", this::register)
            .post("/api/auth/logout", this::logout)
            .get("/api/auth/me", this::me)
            .end();
    }
    
    void login(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        
        if (username == null || password == null) {
            ctx.fail(400, -1, "用户名和密码不能为空");
            return;
        }
        
        String token = Services.auth.login(username, password);
        if (token != null) {
            ctx.ok(Map.of("token", token, "username", username));
        } else {
            ctx.fail(401, -1, "用户名或密码错误");
        }
    }
    
    void register(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        
        if (username == null || password == null || username.length() < 3 || password.length() < 6) {
            ctx.fail(400, -1, "用户名至少3位，密码至少6位");
            return;
        }
        
        if (Services.auth.register(username, password)) {
            ctx.ok("注册成功");
        } else {
            ctx.fail(400, -1, "用户名已存在");
        }
    }
    
    void logout(Context ctx) {
        String token = ctx.headers.get("Authorization");
        if (token != null) {
            Services.auth.logout(token.replace("Bearer ", ""));
        }
        ctx.ok("已登出");
    }
    
    void me(Context ctx) {
        String username = (String) ctx.state.get("username");
        ctx.ok(Map.of("username", username));
    }
}
