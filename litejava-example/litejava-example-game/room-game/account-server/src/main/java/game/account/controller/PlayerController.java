package game.account.controller;

import game.account.AccountException;
import game.account.entity.PlayerEntity;
import game.account.entity.SignInConfigEntity;
import game.account.entity.SignInRecordEntity;
import game.account.service.PlayerService;
import game.account.service.SignInService;
import litejava.App;

import java.util.*;

/**
 * 玩家控制器
 */
public class PlayerController {
    
    public static void register(App app) {
        // 获取玩家信息
        app.get("/player/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            PlayerEntity player = PlayerService.get(userId);
            if (player == null) {
                AccountException.error(1, "玩家不存在");
            }
            ctx.ok(player);
        });
        
        // 签到状态
        app.get("/signin/status/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            SignInRecordEntity record = SignInService.getStatus(userId);
            List<SignInConfigEntity> configs = SignInService.getRewardConfigs();
            
            Map<String, Object> data = new HashMap<>();
            data.put("signDay", record.signDay);
            data.put("todaySigned", SignInService.isTodaySigned(record));
            data.put("rewards", configs);
            ctx.ok(data);
        });
        
        // 执行签到
        app.post("/signin/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            SignInService.SignInResult result = SignInService.signIn(userId);
            
            if (!result.success) {
                AccountException.error(1, result.msg);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("day", result.day);
            data.put("coins", result.coins);
            data.put("diamonds", result.diamonds);
            ctx.ok(data);
        });
    }
}
