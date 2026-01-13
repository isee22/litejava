package game.account.controller;

import game.account.Services;
import game.account.vo.ChatMsgVO;
import game.account.vo.ChatSendReqVO;
import litejava.App;

import java.util.List;

/**
 * 聊天控制器 (HTTP)
 */
public class ChatController {

    public static void register(App app) {
        // 加入聊天室
        app.post("/chat/join", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            Services.chat.joinRoom(req.userId, req.roomId);
            ctx.ok(null);
        });

        // 离开聊天室
        app.post("/chat/leave", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            Services.chat.leaveRoom(req.userId);
            ctx.ok(null);
        });

        // 发送消息，返回需要广播的用户列表和消息
        app.post("/chat/send", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            ChatMsgVO msg = Services.chat.sendMessage(req.userId, req.content);
            ctx.ok(msg);
        });

        // 私聊
        app.post("/chat/private", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            ChatMsgVO msg = Services.chat.sendPrivate(req.userId, req.toId, req.content);
            ctx.ok(msg);
        });

        // 获取聊天室成员
        app.get("/chat/members/:roomId", ctx -> {
            String roomId = ctx.pathParam("roomId");
            List<Long> members = Services.chat.getRoomMembers(roomId);
            ctx.ok(members);
        });
    }
}

