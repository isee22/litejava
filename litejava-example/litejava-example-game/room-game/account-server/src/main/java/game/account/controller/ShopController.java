package game.account.controller;

import game.account.entity.ItemConfigEntity;
import game.account.entity.PlayerItemEntity;
import game.account.service.ItemService;
import game.account.vo.BuyItemReq;
import game.account.vo.UseItemReqVO;
import litejava.App;

import java.util.List;

/**
 * 商城控制器
 */
public class ShopController {
    
    public static void register(App app) {
        // 商城道具列表
        app.get("/shop/items", ctx -> {
            List<ItemConfigEntity> items = ItemService.getShopItems();
            ctx.ok(items);
        });
        
        // 玩家背包
        app.get("/bag/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<PlayerItemEntity> items = ItemService.getPlayerItems(userId);
            ctx.ok(items);
        });
        
        // 购买道具 (钻石)
        app.post("/shop/buy/diamond", ctx -> {
            BuyItemReq req = ctx.bindJSON(BuyItemReq.class);
            ItemService.buyWithDiamond(req.userId, req.itemId, req.count);
            ctx.ok(null);
        });
        
        // 购买道具 (金币)
        app.post("/shop/buy/coin", ctx -> {
            BuyItemReq req = ctx.bindJSON(BuyItemReq.class);
            ItemService.buyWithCoin(req.userId, req.itemId, req.count);
            ctx.ok(null);
        });
        
        // 使用道具
        app.post("/item/use", ctx -> {
            UseItemReqVO req = ctx.bindJSON(UseItemReqVO.class);
            ItemService.useItem(req.userId, req.itemId);
            ctx.ok(null);
        });
    }
}
