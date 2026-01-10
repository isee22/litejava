package authservice.controller;

import authservice.service.AuthService;
import common.BizException;
import common.Err;
import litejava.Context;
import litejava.Routes;

import java.util.Map;

public class AuthController {
    
    public static Routes routes() {
        return new Routes()
            .post("/auth/login", AuthController::login)
            .post("/auth/register", AuthController::register)
            .post("/auth/validate", AuthController::validate)
            .post("/auth/refresh", AuthController::refresh)
            .post("/auth/password", AuthController::changePassword)
            .end();
    }
    
    static void login(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        if (username == null || password == null) BizException.error(Err.PARAM_REQUIRED, "用户名和密码不能为空");
        
        Map<String, Object> result = AuthService.login(username, password);
        ctx.ok(result);
    }
    
    static void register(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        if (username == null || password == null) BizException.error(Err.PARAM_REQUIRED, "用户名和密码不能为空");
        if (email == null || email.isEmpty()) email = username + "@example.com";
        
        Map<String, Object> result = AuthService.registerWithUserCreation(username, password, email, phone);
        ctx.ok(result);
    }
    
    static void validate(Context ctx) {
        String token = ctx.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token == null || token.isEmpty()) {
            Map<String, Object> body = ctx.bindJSON();
            token = (String) body.get("token");
        }
        if (token == null || token.isEmpty()) BizException.paramRequired("Token");
        
        Map<String, Object> result = AuthService.validate(token);
        if (!Boolean.TRUE.equals(result.get("valid"))) BizException.error(Err.TOKEN_EXPIRED, "Token 无效或已过期");
        ctx.ok(result);
    }
    
    static void refresh(Context ctx) {
        String token = ctx.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (token == null || token.isEmpty()) BizException.paramRequired("Token");
        
        String newToken = AuthService.refreshToken(token);
        Map<String, Object> result = Map.of("token", newToken);
        ctx.ok(result);
    }
    
    static void changePassword(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("userId") == null) BizException.paramRequired("userId");
        
        String oldPassword = (String) body.get("oldPassword");
        String newPassword = (String) body.get("newPassword");
        if (oldPassword == null || newPassword == null) BizException.error(Err.PARAM_REQUIRED, "旧密码和新密码不能为空");
        
        Long userId = ((Number) body.get("userId")).longValue();
        AuthService.changePassword(userId, oldPassword, newPassword);
        ctx.ok();
    }
}
