package game.account.controller;

import game.account.entity.GameReplayEntity;
import game.account.service.ReplayService;
import game.account.vo.ReplaySummaryVO;
import litejava.App;

import java.util.List;
import java.util.Map;

/**
 * 录像接口
 */
public class ReplayController {
    
    private final ReplayService replayService;
    
    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }
    
    public void register(App app) {
        // 录像统计 (放在前面，避免被 :replayId 匹配)
        app.get("/replay/stats", ctx -> {
            int total = replayService.count();
            ctx.ok(Map.of("total", total));
        });
        
        // 获取用户录像列表
        app.get("/replay/list/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            int limit = Integer.parseInt(ctx.queryParam("limit", "20"));
            List<ReplaySummaryVO> list = replayService.getUserReplays(userId, limit);
            ctx.ok(list);
        });
        
        // 创建录像 (GameServer 调用)
        app.post("/replay/create", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            String replayId = (String) req.get("replayId");
            String gameType = (String) req.get("gameType");
            String roomId = (String) req.get("roomId");
            List<Number> players = (List<Number>) req.get("players");
            List<String> names = (List<String>) req.get("names");
            String initState = (String) req.getOrDefault("initState", "{}");
            
            List<Long> playerIds = new java.util.ArrayList<>();
            for (Number n : players) {
                playerIds.add(n.longValue());
            }
            
            replayService.create(replayId, gameType, roomId, playerIds, names, initState);
            ctx.ok(null);
        });
        
        // 完成录像 (GameServer 调用)
        app.post("/replay/finish", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            String replayId = (String) req.get("replayId");
            int winner = ((Number) req.get("winner")).intValue();
            String actions = (String) req.getOrDefault("actions", "[]");
            int actionCount = ((Number) req.getOrDefault("actionCount", 0)).intValue();
            
            replayService.finish(replayId, winner, actions, actionCount);
            ctx.ok(null);
        });
        
        // 获取录像详情
        app.get("/replay/:replayId", ctx -> {
            String replayId = ctx.pathParam("replayId");
            GameReplayEntity replay = replayService.get(replayId);
            if (replay == null) {
                ctx.fail(404, "replay not found");
                return;
            }
            ctx.ok(replay);
        });
    }
}
