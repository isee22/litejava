package game.account.service;

import game.account.AccountException;
import game.account.DB;
import game.account.entity.*;
import game.account.mapper.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 道具服务
 */
public class ItemService {
    
    private static List<ItemConfigEntity> configCache;
    private static Map<Integer, ItemConfigEntity> configMap = new ConcurrentHashMap<>();
    private static long cacheTime = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5分钟
    
    /**
     * 获取商城道具列表
     */
    public static List<ItemConfigEntity> getShopItems() {
        refreshCache();
        return configCache;
    }
    
    /**
     * 获取道具配置
     */
    public static ItemConfigEntity getConfig(int itemId) {
        refreshCache();
        return configMap.get(itemId);
    }
    
    private static void refreshCache() {
        long now = System.currentTimeMillis();
        if (configCache != null && now - cacheTime < CACHE_TTL) {
            return;
        }
        
        List<ItemConfigEntity> list = DB.execute(ItemConfigMapper.class, ItemConfigMapper::findAllEnabled);
        if (list == null) {
            list = Collections.emptyList();
        }
        
        configCache = list;
        configMap.clear();
        for (ItemConfigEntity cfg : list) {
            configMap.put(cfg.itemId, cfg);
        }
        cacheTime = now;
    }
    
    /**
     * 获取玩家道具
     */
    public static List<PlayerItemEntity> getPlayerItems(long userId) {
        List<PlayerItemEntity> items = DB.execute(PlayerItemMapper.class, m -> m.findByUserId(userId));
        if (items == null) {
            return Collections.emptyList();
        }
        
        long now = System.currentTimeMillis();
        items.removeIf(item -> item.expireTime > 0 && item.expireTime < now);
        return items;
    }
    
    /**
     * 购买道具 (钻石)
     */
    public static void buyWithDiamond(long userId, int itemId, int count) {
        ItemConfigEntity cfg = getConfig(itemId);
        if (cfg == null) {
            AccountException.error(1, "道具不存在");
        }
        
        int totalPrice = cfg.price * count;
        PlayerEntity player = PlayerService.get(userId);
        if (player == null || player.diamonds < totalPrice) {
            AccountException.error(2, "钻石不足");
        }
        
        PlayerService.addDiamonds(userId, -totalPrice, "购买道具:" + cfg.name);
        addItem(userId, itemId, count, cfg.duration);
    }
    
    /**
     * 购买道具 (金币)
     */
    public static void buyWithCoin(long userId, int itemId, int count) {
        ItemConfigEntity cfg = getConfig(itemId);
        if (cfg == null) {
            AccountException.error(1, "道具不存在");
        }
        
        if (cfg.coinPrice <= 0) {
            AccountException.error(2, "该道具不支持金币购买");
        }
        
        int totalPrice = cfg.coinPrice * count;
        PlayerEntity player = PlayerService.get(userId);
        if (player == null || player.coins < totalPrice) {
            AccountException.error(3, "金币不足");
        }
        
        PlayerService.addCoins(userId, -totalPrice, "购买道具:" + cfg.name);
        addItem(userId, itemId, count, cfg.duration);
    }
    
    /**
     * 使用道具
     */
    public static void useItem(long userId, int itemId) {
        PlayerItemEntity item = DB.execute(PlayerItemMapper.class, m -> m.findByUserAndItem(userId, itemId));
        if (item == null || item.count <= 0) {
            AccountException.error(1, "没有该道具");
        }
        
        ItemConfigEntity cfg = getConfig(itemId);
        if (cfg == null) {
            AccountException.error(2, "道具配置不存在");
        }
        
        item.count--;
        item.updateTime = System.currentTimeMillis();
        
        if (item.count <= 0) {
            DB.execute(PlayerItemMapper.class, m -> m.delete(item.id));
        } else {
            DB.execute(PlayerItemMapper.class, m -> m.update(item));
        }
    }
    
    /**
     * 添加道具
     */
    public static void addItem(long userId, int itemId, int count, int duration) {
        PlayerItemEntity existing = DB.execute(PlayerItemMapper.class, m -> m.findByUserAndItem(userId, itemId));
        long now = System.currentTimeMillis();
        
        if (existing != null) {
            existing.count += count;
            if (duration > 0 && existing.expireTime > 0) {
                existing.expireTime += duration * 1000L;
            }
            existing.updateTime = now;
            DB.execute(PlayerItemMapper.class, m -> m.update(existing));
        } else {
            PlayerItemEntity item = new PlayerItemEntity();
            item.userId = userId;
            item.itemId = itemId;
            item.count = count;
            item.expireTime = duration > 0 ? now + duration * 1000L : 0;
            item.createTime = now;
            item.updateTime = now;
            DB.execute(PlayerItemMapper.class, m -> m.insert(item));
        }
    }
    
    /**
     * 检查玩家是否有某类型的有效道具
     */
    public static ItemConfigEntity getActiveItem(long userId, String type) {
        List<PlayerItemEntity> items = getPlayerItems(userId);
        long now = System.currentTimeMillis();
        
        for (PlayerItemEntity pi : items) {
            if (pi.count <= 0) continue;
            if (pi.expireTime > 0 && pi.expireTime < now) continue;
            
            ItemConfigEntity cfg = getConfig(pi.itemId);
            if (cfg != null && cfg.type.equals(type)) {
                return cfg;
            }
        }
        return null;
    }
}
