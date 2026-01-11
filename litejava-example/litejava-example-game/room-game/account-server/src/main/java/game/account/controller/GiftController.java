package game.account.controller;

import game.account.service.GiftService;
import game.account.vo.CharmVO;
import game.account.vo.SendGiftReq;
import litejava.App;

import java.util.List;

/**
 * 礼物控制器
 */
public class GiftController {
    
    public static void register(App app) {
        // 送礼物
        app.post("/gift/send", ctx -> {
            SendGiftReq req = ctx.bindJSON(SendGiftReq.class);
            GiftService.sendGift(req.fromUserId, req.toUserId, req.giftId, req.count);
            ctx.ok(null);
        });
        
        // 获取魅力值
        app.get("/gift/charm/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            int charm = GiftService.getCharm(userId);
            ctx.ok(new CharmVO(charm));
        });
        
        // 收到的礼物
        app.get("/gift/received/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<GiftService.GiftRecord> gifts = GiftService.getReceivedGifts(userId, 50);
            ctx.ok(gifts);
        });
        
        // 送出的礼物
        app.get("/gift/sent/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<GiftService.GiftRecord> gifts = GiftService.getSentGifts(userId, 50);
            ctx.ok(gifts);
        });
    }
}
