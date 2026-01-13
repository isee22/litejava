package game.account.controller;

import game.common.GameException;
import game.account.Services;
import game.account.entity.GameConfig;
import litejava.App;

import java.util.*;

/**
 * 配置控制器
 */
public class ConfigController {

    public static void register(App app) {
        // 游戏配置列表 (所有启用的场次)
        app.get("/room/config", ctx -> {
            List<GameConfig> list = Services.config.findAllEnabled();
            ctx.ok(list);
        });

        // 某游戏的所有场次
        app.get("/room/config/:gameType", ctx -> {
            String gameType = ctx.pathParam("gameType");
            List<GameConfig> list = Services.config.findByGameType(gameType);
            ctx.ok(list);
        });

        // 某游戏某场次配置
        app.get("/room/config/:gameType/:roomLevel", ctx -> {
            String gameType = ctx.pathParam("gameType");
            int roomLevel = Integer.parseInt(ctx.pathParam("roomLevel"));
            GameConfig config = Services.config.findByTypeAndLevel(gameType, roomLevel);
            if (config == null) {
                GameException.error(1, "场次配置不存在");
            }
            ctx.ok(config);
        });

        // 刷新配置到缓存 (管理接口)
        app.post("/admin/config/refresh", ctx -> {
            Services.config.refresh();
            ctx.ok(Map.of("message", "config refreshed"));
        });
    }
}

