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
                .summary("用户登录")
                .desc("使用用户名和密码登录，返回 JWT token")
                .tags("认证")
                .param("username", String.class, "用户名")
                .param("password", String.class, "密码")
                .response(200, Map.class, "登录成功，返回 token")
                .response(401, Map.class, "用户名或密码错误")
            .post("/api/auth/register", this::register)
                .summary("用户注册")
                .desc("注册新用户，用户名至少3位，密码至少6位")
                .tags("认证")
                .param("username", String.class, "用户名（至少3位）")
                .param("password", String.class, "密码（至少6位）")
                .response(200, Map.class, "注册成功")
                .response(400, Map.class, "参数错误或用户名已存在")
            .post("/api/auth/logout", this::logout)
                .summary("用户登出")
                .tags("认证")
                .response(200, Map.class, "登出成功")
            .get("/api/auth/me", this::me)
                .summary("获取当前用户")
                .desc("需要 Authorization header 携带 token")
                .tags("认证")
                .response(200, Map.class, "当前用户信息")
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
