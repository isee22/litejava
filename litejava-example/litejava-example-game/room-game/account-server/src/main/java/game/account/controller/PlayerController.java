package game.account.controller;

import game.common.GameException;
import game.account.Services;
import game.account.entity.Player;
import game.account.entity.SignInConfig;
import game.account.entity.SignInRecord;
import game.account.vo.SignInResultVO;
import game.account.vo.SignInStatusVO;
import litejava.App;

import java.util.List;
import java.util.Map;

/**
 * 玩家控制器
 */
public class PlayerController {

    @SuppressWarnings("unchecked")
    public static void register(App app) {
        // 获取玩家信息
        app.get("/player/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            Player player = Services.player.get(userId);
            if (player == null) {
                GameException.error(1, "玩家不存在");
            }
            ctx.ok(player);
        });
        
        // 游戏结算 (GameServer 调用)
        app.post("/game/settle", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            String roomId = (String) req.get("roomId");
            String gameType = (String) req.get("gameType");
            List<Map<String, Object>> settlements = (List<Map<String, Object>>) req.get("settlements");
            
            if (settlements == null || settlements.isEmpty()) {
                ctx.fail(1, "settlements is empty");
                return;
            }
            
            Services.player.batchSettle(settlements);
            app.log.info("游戏结算完成: roomId=" + roomId + ", gameType=" + gameType + ", players=" + settlements.size());
            ctx.ok(null);
        });
        
        // 记录逃跑 (GameServer 调用)
        app.post("/escape", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = ((Number) req.get("userId")).longValue();
            String roomId = (String) req.get("roomId");
            String gameType = (String) req.get("gameType");
            
            Services.player.recordEscape(userId);
            app.log.info("记录逃跑: userId=" + userId + ", roomId=" + roomId + ", gameType=" + gameType);
            ctx.ok(null);
        });

        // 签到状态
        app.get("/signin/status/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            SignInRecord record = Services.signIn.getStatus(userId);
            List<SignInConfig> configs = Services.signIn.getRewardConfigs();

            SignInStatusVO vo = new SignInStatusVO();
            vo.signDay = record.signDay;
            vo.todaySigned = Services.signIn.isTodaySigned(record);
            vo.rewards = configs;
            ctx.ok(vo);
        });

        // 执行签到
        app.post("/signin/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            SignInResultVO result = Services.signIn.signIn(userId);

            if (!result.success) {
                GameException.error(1, result.msg);
            }
            ctx.ok(result);
        });
    }
}

