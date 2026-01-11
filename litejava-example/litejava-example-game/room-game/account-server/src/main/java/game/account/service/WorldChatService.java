package game.account.service;

import game.account.AccountException;
import game.account.entity.ItemConfigEntity;
import game.account.entity.PlayerEntity;
import game.account.vo.WorldChatMsgVO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全服广播服务
 */
public class WorldChatService {
    
    /** 喇叭道具ID */
    public static final int HORN_ITEM_ID = 4001;
    
    /** 冷却时间(毫秒) */
    private static final long COOLDOWN_MS = 10_000;
    
    /** 消息最大长度 */
    private static final int MAX_LENGTH = 100;
    
    /** 用户冷却记录: userId -> lastSendTime */
    private static final Map<Long, Long> cooldowns = new ConcurrentHashMap<>();
    
    /**
     * 发送全服广播
     * @return 广播消息VO
     */
    public static WorldChatMsgVO send(long userId, String content) {
        // 校验内容
        if (content == null || content.isBlank()) {
            AccountException.error(1, "消息内容不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            AccountException.error(2, "消息内容过长，最多" + MAX_LENGTH + "字");
        }
        
        // 检查冷却
        long now = System.currentTimeMillis();
        Long lastTime = cooldowns.get(userId);
        if (lastTime != null && now - lastTime < COOLDOWN_MS) {
            long remain = (COOLDOWN_MS - (now - lastTime)) / 1000;
            AccountException.error(3, "发言冷却中，请等待" + remain + "秒");
        }
        
        // 检查并扣除喇叭道具
        ItemService.useItem(userId, HORN_ITEM_ID);
        
        // 更新冷却
        cooldowns.put(userId, now);
        
        // 构建消息
        PlayerEntity player = PlayerService.get(userId);
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
