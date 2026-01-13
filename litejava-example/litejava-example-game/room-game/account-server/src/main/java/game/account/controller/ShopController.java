package game.account.controller;

import game.account.Services;
import game.account.entity.ItemConfig;
import game.account.entity.PlayerItem;
import game.account.vo.BuyItemReq;
import game.account.vo.UseItemReqVO;
import litejava.App;

import java.util.List;

/**
 * 商城控制器
 */
public class ShopController {

    public static void register(App app) {
        app.get("/shop/items", ctx -> {
            List<ItemConfig> items = Services.item.getShopItems();
            ctx.ok(items);
        });

        app.get("/bag/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<PlayerItem> items = Services.item.getPlayerItems(userId);
            ctx.ok(items);
        });

        app.post("/shop/buy/diamond", ctx -> {
            BuyItemReq req = ctx.bindJSON(BuyItemReq.class);
            Services.item.buyWithDiamond(req.userId, req.itemId, req.count);
            ctx.ok(null);
        });

        app.post("/shop/buy/coin", ctx -> {
            BuyItemReq req = ctx.bindJSON(BuyItemReq.class);
            Services.item.buyWithCoin(req.userId, req.itemId, req.count);
            ctx.ok(null);
        });

        app.post("/item/use", ctx -> {
            UseItemReqVO req = ctx.bindJSON(UseItemReqVO.class);
            Services.item.useItem(req.userId, req.itemId);
            ctx.ok(null);
        });

        app.post("/item/consume", ctx -> {
            java.util.Map<String, Object> req = ctx.bindJSON();
            long userId = ((Number) req.get("userId")).longValue();
            int itemId = ((Number) req.get("itemId")).intValue();
            int count = req.get("count") != null ? ((Number) req.get("count")).intValue() : 1;
            
            boolean success = Services.item.consumeItem(userId, itemId, count);
            if (!success) {
                ctx.fail(1, "道具不足");
                return;
            }
            ctx.ok(null);
        });
    }
}

