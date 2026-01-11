package game.account.controller;

import game.account.AccountException;
import game.account.DB;
import game.account.entity.GameConfigEntity;
import game.account.mapper.GameConfigMapper;
import litejava.App;

import java.util.*;

/**
 * 配置控制器
 */
public class ConfigController {
    
    public static void register(App app) {
        // 游戏配置列表 (所有启用的场次)
        app.get("/room/config", ctx -> {
            List<GameConfigEntity> list = DB.execute(GameConfigMapper.class, GameConfigMapper::findAllEnabled);
            ctx.ok(list);
        });
        
        // 某游戏的所有场次
        app.get("/room/config/:gameType", ctx -> {
            String gameType = ctx.pathParam("gameType");
            List<GameConfigEntity> list = DB.execute(GameConfigMapper.class, m -> m.findByGameType(gameType));
            ctx.ok(list);
        });
        
        // 某游戏某场次配置
        app.get("/room/config/:gameType/:roomLevel", ctx -> {
            String gameType = ctx.pathParam("gameType");
            String roomLevel = ctx.pathParam("roomLevel");
            GameConfigEntity config = DB.execute(GameConfigMapper.class, m -> m.findByTypeAndLevel(gameType, roomLevel));
            if (config == null) {
                AccountException.error(1, "场次配置不存在");
            }
            ctx.ok(config);
        });
    }
}
