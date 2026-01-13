package game.account.service;

import game.common.GameException;
import game.account.Services;
import game.account.entity.Player;
import game.account.vo.WorldChatMsgVO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全服广播服务
 */
public class WorldChatService {

    public static final int HORN_ITEM_ID = 4001;
    private static final long COOLDOWN_MS = 10_000;
    private static final int MAX_LENGTH = 100;
    private final Map<Long, Long> cooldowns = new ConcurrentHashMap<>();

    public WorldChatMsgVO send(long userId, String content) {
        if (content == null || content.isBlank()) {
            GameException.error(1, "消息内容不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            GameException.error(2, "消息内容过长，最多" + MAX_LENGTH + "字");
        }

        long now = System.currentTimeMillis();
        Long lastTime = cooldowns.get(userId);
        if (lastTime != null && now - lastTime < COOLDOWN_MS) {
            long remain = (COOLDOWN_MS - (now - lastTime)) / 1000;
            GameException.error(3, "发言冷却中，请等待" + remain + "秒");
        }

        Services.item.useItem(userId, HORN_ITEM_ID);
        cooldowns.put(userId, now);

        Player player = Services.player.get(userId);
        WorldChatMsgVO msg = new WorldChatMsgVO();
        msg.userId = userId;
        msg.name = player != null ? player.name : "玩家" + userId;
        msg.avatar = player != null ? player.avatar : null;
        msg.vipLevel = player != null ? player.vipLevel : 0;
        msg.content = content;
        msg.time = now;

        return msg;
    }
}

