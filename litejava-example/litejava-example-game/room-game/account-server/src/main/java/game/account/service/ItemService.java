package game.account.service;

import game.common.GameException;
import game.account.Services;
import game.account.dao.ItemDao;
import game.account.entity.ItemConfig;
import game.account.entity.Player;
import game.account.entity.PlayerItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 道具服务
 */
public class ItemService {

    private final ItemDao itemDao = new ItemDao();
    private Map<Integer, ItemConfig> configMap;

    public List<ItemConfig> getShopItems() {
        return getConfigs();
    }

    public ItemConfig getConfig(int itemId) {
        loadConfigs();
        return configMap.get(itemId);
    }

    private List<ItemConfig> getConfigs() {
        loadConfigs();
        return itemDao.findAllConfigs();
    }

    private void loadConfigs() {
        if (configMap != null) return;

        List<ItemConfig> list = itemDao.findAllConfigs();
        if (list == null) list = Collections.emptyList();

        configMap = new ConcurrentHashMap<>();
        for (ItemConfig cfg : list) {
            configMap.put(cfg.itemId, cfg);
        }
    }

    public List<PlayerItem> getPlayerItems(long userId) {
        List<PlayerItem> items = itemDao.findByUserId(userId);
        if (items == null) return Collections.emptyList();

        long now = System.currentTimeMillis();
        items.removeIf(item -> item.expireTime > 0 && item.expireTime < now);
        return items;
    }

    public void buyWithDiamond(long userId, int itemId, int count) {
        ItemConfig cfg = getConfig(itemId);
        if (cfg == null) {
            GameException.error(1, "道具不存在");
        }

        int totalPrice = cfg.price * count;
        Player player = Services.player.get(userId);
        if (player == null || player.diamonds < totalPrice) {
            GameException.error(2, "钻石不足");
        }

        Services.player.addDiamonds(userId, -totalPrice, "购买道具:" + cfg.name);
        addItem(userId, itemId, count, cfg.duration);
    }

    public void buyWithCoin(long userId, int itemId, int count) {
        ItemConfig cfg = getConfig(itemId);
        if (cfg == null) {
            GameException.error(1, "道具不存在");
        }
        if (cfg.coinPrice <= 0) {
            GameException.error(2, "该道具不支持金币购买");
        }

        int totalPrice = cfg.coinPrice * count;
        Player player = Services.player.get(userId);
        if (player == null || player.coins < totalPrice) {
            GameException.error(3, "金币不足");
        }

        Services.player.addCoins(userId, -totalPrice, "购买道具:" + cfg.name);
        addItem(userId, itemId, count, cfg.duration);
    }

    public void useItem(long userId, int itemId) {
        PlayerItem item = itemDao.findByUserAndItem(userId, itemId);
        if (item == null || item.count <= 0) {
            GameException.error(1, "没有该道具");
        }

        ItemConfig cfg = getConfig(itemId);
        if (cfg == null) {
            GameException.error(2, "道具配置不存在");
        }

        item.count--;
        item.updateTime = System.currentTimeMillis();

        if (item.count <= 0) {
            itemDao.deleteItem(item.id);
        } else {
            itemDao.updateItem(item);
        }
    }

    public boolean consumeItem(long userId, int itemId, int count) {
        PlayerItem item = itemDao.findByUserAndItem(userId, itemId);
        if (item == null || item.count < count) {
            return false;
        }

        item.count -= count;
        item.updateTime = System.currentTimeMillis();

        if (item.count <= 0) {
            itemDao.deleteItem(item.id);
        } else {
            itemDao.updateItem(item);
        }
        return true;
    }

    public int getItemCount(long userId, int itemId) {
        PlayerItem item = itemDao.findByUserAndItem(userId, itemId);
        if (item == null) return 0;
        
        long now = System.currentTimeMillis();
        if (item.expireTime > 0 && item.expireTime < now) {
            return 0;
        }
        return item.count;
    }

    public void addItem(long userId, int itemId, int count, int duration) {
        PlayerItem existing = itemDao.findByUserAndItem(userId, itemId);
        long now = System.currentTimeMillis();

        if (existing != null) {
            existing.count += count;
            if (duration > 0 && existing.expireTime > 0) {
                existing.expireTime += duration * 1000L;
            }
            existing.updateTime = now;
            itemDao.updateItem(existing);
        } else {
            PlayerItem item = new PlayerItem();
            item.userId = userId;
            item.itemId = itemId;
            item.count = count;
            item.expireTime = duration > 0 ? now + duration * 1000L : 0;
            item.createTime = now;
            item.updateTime = now;
            itemDao.insertItem(item);
        }
    }

    public ItemConfig getActiveItem(long userId, String type) {
        List<PlayerItem> items = getPlayerItems(userId);
        long now = System.currentTimeMillis();

        for (PlayerItem pi : items) {
            if (pi.count <= 0) continue;
            if (pi.expireTime > 0 && pi.expireTime < now) continue;

            ItemConfig cfg = getConfig(pi.itemId);
            if (cfg != null && cfg.type.equals(type)) {
                return cfg;
            }
        }
        return null;
    }
}

