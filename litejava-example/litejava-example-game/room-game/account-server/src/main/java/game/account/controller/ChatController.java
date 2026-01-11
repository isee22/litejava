package game.account.controller;

import game.account.service.ChatService;
import game.account.vo.ChatMsgVO;
import game.account.vo.ChatSendReqVO;
import litejava.App;

import java.util.List;

/**
 * 聊天控制器 (HTTP)
 * 
 * Gateway 转发聊天请求到这里，获取需要广播的消息列表
 */
public class ChatController {
    
    public static void register(App app) {
        // 加入聊天室
        app.post("/chat/join", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            ChatService.joinRoom(req.userId, req.roomId);
            ctx.ok(null);
        });
        
        // 离开聊天室
        app.post("/chat/leave", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            ChatService.leaveRoom(req.userId);
            ctx.ok(null);
        });
        
        // 发送消息，返回需要广播的用户列表和消息
        app.post("/chat/send", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            ChatMsgVO msg = ChatService.sendMessage(req.userId, req.content);
            ctx.ok(msg);
        });
        
        // 私聊
        app.post("/chat/private", ctx -> {
            ChatSendReqVO req = ctx.bindJSON(ChatSendReqVO.class);
            ChatMsgVO msg = ChatService.sendPrivate(req.userId, req.toId, req.content);
            ctx.ok(msg);
        });
        
        // 获取聊天室成员
        app.get("/chat/members/:roomId", ctx -> {
            String roomId = ctx.pathParam("roomId");
            List<Long> members = ChatService.getRoomMembers(roomId);
            ctx.ok(members);
        });
    }
}
