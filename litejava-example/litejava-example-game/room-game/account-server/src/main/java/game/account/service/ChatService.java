package game.account.service;

import game.common.GameException;
import game.account.Services;
import game.account.entity.Player;
import game.account.vo.ChatMsgVO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务
 */
public class ChatService {

    private final Map<String, Set<Long>> chatRooms = new ConcurrentHashMap<>();
    private final Map<Long, String> userRooms = new ConcurrentHashMap<>();

    public void joinRoom(long userId, String roomId) {
        if (roomId == null || roomId.isBlank()) {
            roomId = "lobby";
        }
        leaveRoom(userId);
        userRooms.put(userId, roomId);
        chatRooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void leaveRoom(long userId) {
        String roomId = userRooms.remove(userId);
        if (roomId != null) {
            Set<Long> members = chatRooms.get(roomId);
            if (members != null) {
                members.remove(userId);
            }
        }
    }

    public ChatMsgVO sendMessage(long userId, String content) {
        String roomId = userRooms.get(userId);
        if (roomId == null) {
            GameException.error(1, "未加入聊天室");
        }
        if (content == null || content.isBlank()) {
            GameException.error(2, "消息内容不能为空");
        }

        Player player = Services.player.get(userId);
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

    public ChatMsgVO sendPrivate(long fromId, long toId, String content) {
        if (content == null || content.isBlank()) {
            GameException.error(1, "消息内容不能为空");
        }

        Player player = Services.player.get(fromId);
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

    public List<Long> getRoomMembers(String roomId) {
        Set<Long> members = chatRooms.get(roomId);
        if (members == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(members);
    }

    public String getUserRoom(long userId) {
        return userRooms.get(userId);
    }
}

