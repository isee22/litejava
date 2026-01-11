package game.account.controller;

import game.account.service.RankService;
import game.account.vo.PlayerRankVO;
import game.account.vo.RankItemVO;
import litejava.App;

import java.util.List;

/**
 * 排行榜控制器
 */
public class RankController {
    
    public static void register(App app) {
        // 金币排行榜
        app.get("/rank/coins", ctx -> {
            String limitStr = ctx.queryParam("limit");
            int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;
            List<RankItemVO> rank = RankService.getRank(RankService.RANK_COINS, limit);
            ctx.ok(rank);
        });
        
        // 胜场排行榜
        app.get("/rank/wins", ctx -> {
            String limitStr = ctx.queryParam("limit");
            int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;
            List<RankItemVO> rank = RankService.getRank(RankService.RANK_WINS, limit);
            ctx.ok(rank);
        });
        
        // 等级排行榜
        app.get("/rank/level", ctx -> {
            String limitStr = ctx.queryParam("limit");
            int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;
            List<RankItemVO> rank = RankService.getRank(RankService.RANK_LEVEL, limit);
            ctx.ok(rank);
        });
        
        // 魅力排行榜
        app.get("/rank/charm", ctx -> {
            String limitStr = ctx.queryParam("limit");
            int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;
            List<RankItemVO> rank = RankService.getRank(RankService.RANK_CHARM, limit);
            ctx.ok(rank);
        });
        
        // 获取玩家排名
        app.get("/rank/:type/:userId", ctx -> {
            String type = ctx.pathParam("type");
            long userId = Long.parseLong(ctx.pathParam("userId"));
            int rank = RankService.getPlayerRank(type, userId);
            ctx.ok(new PlayerRankVO(rank));
        });
    }
}
