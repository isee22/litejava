package game.account.controller;

import game.account.Services;
import game.account.vo.*;
import litejava.App;

import java.util.List;

/**
 * 好友控制器
 */
public class FriendController {

    public static void register(App app) {
        app.get("/friend/list/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<FriendVO> friends = Services.friend.getFriends(userId);
            ctx.ok(friends);
        });

        app.get("/friend/requests/:userId", ctx -> {
            long userId = Long.parseLong(ctx.pathParam("userId"));
            List<FriendRequestVO> requests = Services.friend.getPendingRequests(userId);
            ctx.ok(requests);
        });

        app.post("/friend/request", ctx -> {
            FriendReqVO req = ctx.bindJSON(FriendReqVO.class);
            Services.friend.sendRequest(req.fromId, req.toId, req.message);
            ctx.ok(new ResultVO("请求已发送"));
        });

        app.post("/friend/accept", ctx -> {
            FriendAcceptReqVO req = ctx.bindJSON(FriendAcceptReqVO.class);
            Services.friend.acceptRequest(req.userId, req.requestId);
            ctx.ok(new ResultVO("已添加好友"));
        });

        app.post("/friend/reject", ctx -> {
            FriendAcceptReqVO req = ctx.bindJSON(FriendAcceptReqVO.class);
            Services.friend.rejectRequest(req.userId, req.requestId);
            ctx.ok(new ResultVO("已拒绝"));
        });

        app.post("/friend/delete", ctx -> {
            FriendDeleteReqVO req = ctx.bindJSON(FriendDeleteReqVO.class);
            Services.friend.deleteFriend(req.userId, req.friendId);
            ctx.ok(new ResultVO("已删除好友"));
        });
    }
}

