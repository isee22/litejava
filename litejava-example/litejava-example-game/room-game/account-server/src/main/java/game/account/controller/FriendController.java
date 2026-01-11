package game.account.controller;

import game.account.service.FriendService;
import game.account.vo.*;
import litejava.App;

import java.util.List;

/**
 * 好友控制器
 */
public class FriendController {
    
    public static void register(App app) {
        // 获取好友列表
        app.get("/friend/list/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<FriendVO> friends = FriendService.getFriends(userId);
            ctx.ok(friends);
        });
        
        // 获取待处理的好友请求
        app.get("/friend/requests/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<FriendRequestVO> requests = FriendService.getPendingRequests(userId);
            ctx.ok(requests);
        });
        
        // 发送好友请求
        app.post("/friend/request", ctx -> {
            FriendReqVO req = ctx.bindJSON(FriendReqVO.class);
            FriendService.sendRequest(req.fromId, req.toId, req.message);
            ctx.ok(new ResultVO("请求已发送"));
        });
        
        // 接受好友请求
        app.post("/friend/accept", ctx -> {
            FriendAcceptReqVO req = ctx.bindJSON(FriendAcceptReqVO.class);
            FriendService.acceptRequest(req.userId, req.requestId);
            ctx.ok(new ResultVO("已添加好友"));
        });
        
        // 拒绝好友请求
        app.post("/friend/reject", ctx -> {
            FriendAcceptReqVO req = ctx.bindJSON(FriendAcceptReqVO.class);
            FriendService.rejectRequest(req.userId, req.requestId);
            ctx.ok(new ResultVO("已拒绝"));
        });
        
        // 删除好友
        app.post("/friend/delete", ctx -> {
            FriendDeleteReqVO req = ctx.bindJSON(FriendDeleteReqVO.class);
            FriendService.deleteFriend(req.userId, req.friendId);
            ctx.ok(new ResultVO("已删除好友"));
        });
    }
}
