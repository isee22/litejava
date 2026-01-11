package game.account.service;

import game.account.AccountException;
import game.account.vo.ChatMsgVO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务
 */
public class ChatService {
    
    // 聊天室: roomId -> Set<userId>
    private static final Map<String, Set<Long>> chatRooms = new ConcurrentHashMap<>();
    // 用户所在聊天室: userId -> roomId
    private static final Map<Long, String> userRooms = new ConcurrentHashMap<>();
    
    /**
     * 加入聊天室
     */
    public static void joinRoom(long userId, String roomId) {
        if (roomId == null || roomId.isBlank()) {
            roomId = "lobby";
        }
        
        // 先离开旧聊天室
        leaveRoom(userId);
        
        // 加入新聊天室
        userRooms.put(userId, roomId);
        chatRooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }
    
    /**
     * 离开聊天室
     */
    public static void leaveRoom(long userId) {
        String roomId = userRooms.remove(userId);
        if (roomId != null) {
            Set<Long> members = chatRooms.get(roomId);
            if (members != null) {
                members.remove(userId);
            }
        }
    }
    
    /**
     * 发送消息
     */
    public static ChatMsgVO sendMessage(long userId, String content) {
        String roomId = userRooms.get(userId);
        if (roomId == null) {
            AccountException.error(1, "未加入聊天室");
        }
        
        if (content == null || content.isBlank()) {
            AccountException.error(2, "消息内容不能为空");
        }
        
        // 获取发送者信息
        game.account.entity.PlayerEntity player = PlayerService.get(userId);
        String name = player != null ? player.name : "玩家" + userId;
        
        ChatMsgVO msg = new ChatMsgVO();
        msg.userId = userId;
        msg.name = name;
        msg.content = content;
        msg.roomId = roomId;
        msg.time = System.currentTimeMillis();
        msg.recipients = new ArrayList<>(getRoomMembers(roomId));
        
        return msg;
    }
    
    /**
     * 私聊
     */
    public static ChatMsgVO sendPrivate(long fromId, long toId, String content) {
        if (content == null || content.isBlank()) {
            AccountException.error(1, "消息内容不能为空");
        }
        
        game.account.entity.PlayerEntity player = PlayerService.get(fromId);
        String name = player != null ? player.name : "玩家" + fromId;
        
        ChatMsgVO msg = new ChatMsgVO();
        msg.userId = fromId;
        msg.name = name;
        msg.content = content;
        msg.toId = toId;
        msg.time = System.currentTimeMillis();
        msg.recipients = List.of(toId);
        msg.isPrivate = true;
        
        return msg;
    }
    
    /**
     * 获取聊天室成员
     */
    public static List<Long> getRoomMembers(String roomId) {
        Set<Long> members = chatRooms.get(roomId);
        if (members == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(members);
    }
    
    /**
     * 获取用户所在聊天室
     */
    public static String getUserRoom(long userId) {
        return userRooms.get(userId);
    }
}
