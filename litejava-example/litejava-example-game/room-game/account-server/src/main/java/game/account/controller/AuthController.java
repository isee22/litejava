package game.account.controller;

import game.account.entity.AccountEntity;
import game.account.entity.PlayerEntity;
import game.account.service.AuthService;
import game.account.service.PlayerService;
import game.account.vo.LoginReq;
import game.account.vo.LoginResp;
import game.account.vo.RegisterReq;
import litejava.App;

/**
 * 认证控制器 (HTTP)
 * 
 * Nginx 转发 /api/account/* -> AccountServer (去掉前缀)
 */
public class AuthController {
    
    public static void register(App app) {
        // 注册
        app.post("/register", ctx -> {
            RegisterReq req = ctx.bindJSON(RegisterReq.class);
            AccountEntity account = AuthService.register(req.username, req.password);
            String name = req.name != null ? req.name : req.username;
            PlayerEntity player = PlayerService.getOrCreate(account.id, name);
            
            LoginResp resp = new LoginResp();
            resp.userId = account.id;
            resp.username = account.username;
            resp.name = player.name;
            resp.coins = player.coins;
            resp.diamonds = player.diamonds;
            ctx.ok(resp);
        });
        
        // 登录
        app.post("/login", ctx -> {
            LoginReq req = ctx.bindJSON(LoginReq.class);
            AccountEntity account = AuthService.login(req.username, req.password);
            PlayerEntity player = PlayerService.get(account.id);
            
            LoginResp resp = new LoginResp();
            resp.userId = account.id;
            resp.username = account.username;
            resp.name = player.name;
            resp.coins = player.coins;
            resp.diamonds = player.diamonds;
            ctx.ok(resp);
        });
    }
}
