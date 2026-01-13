package game.account.controller;

import game.account.Services;
import game.account.entity.Account;
import game.account.entity.Player;
import game.account.vo.LoginReq;
import game.account.vo.LoginResp;
import game.account.vo.RegisterReq;
import litejava.App;

/**
 * 认证控制器
 */
public class AuthController {

    public static void register(App app) {
        // 注册
        app.post("/register", ctx -> {
            RegisterReq req = ctx.bindJSON(RegisterReq.class);
            Account account = Services.auth.register(req.username, req.password);
            String name = req.name != null ? req.name : req.username;
            Player player = Services.player.getOrCreate(account.id, name);
            
            // 注册送1000房卡
            try {
                Services.item.addItem(account.id, 5001, 1000, 0);
                app.log.info("注册送房卡成功: userId=" + account.id);
            } catch (Exception e) {
                app.log.error("注册送房卡失败: " + e.getMessage(), e);
            }

            LoginResp resp = new LoginResp();
            resp.userId = account.id;
            resp.username = account.username;
            resp.name = player.name;
            resp.coins = player.coins;
            resp.diamonds = player.diamonds;
            resp.roomConfigs = Services.config.findAllEnabled();
            resp.items = Services.item.getPlayerItems(account.id);
            ctx.ok(resp);
        });

        // 登录
        app.post("/login", ctx -> {
            LoginReq req = ctx.bindJSON(LoginReq.class);
            Account account = Services.auth.login(req.username, req.password);
            Player player = Services.player.get(account.id);

            LoginResp resp = new LoginResp();
            resp.userId = account.id;
            resp.username = account.username;
            resp.name = player.name;
            resp.coins = player.coins;
            resp.diamonds = player.diamonds;
            resp.roomConfigs = Services.config.findAllEnabled();
            resp.items = Services.item.getPlayerItems(account.id);
            
            // 检查断线重连 (BabyKylin: roomid)
            Services.reconnect.fillReconnectInfo(resp);
            
            ctx.ok(resp);
        });
    }
}

