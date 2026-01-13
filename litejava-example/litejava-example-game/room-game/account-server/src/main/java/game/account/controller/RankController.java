package game.account.controller;

import game.account.Services;
import game.account.vo.RankVO;
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
            List<RankVO> rank = Services.rank.getCoinRank(limit);
            ctx.ok(rank);
        });

        // 胜场排行榜
        app.get("/rank/wins", ctx -> {
            String limitStr = ctx.queryParam("limit");
            int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;
            List<RankVO> rank = Services.rank.getWinRank(limit);
            ctx.ok(rank);
        });
    }
}

