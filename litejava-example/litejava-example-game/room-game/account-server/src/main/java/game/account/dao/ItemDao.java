package game.account.dao;

import game.account.Cache;
import game.account.DB;
import game.account.entity.ItemConfig;
import game.account.entity.PlayerItem;
import game.account.mapper.ItemConfigMapper;
import game.account.mapper.PlayerItemMapper;

import java.util.List;

/**
 * 道具 DAO
 */
public class ItemDao {

    private static final String KEY_CONFIG = "item:config";

    // ========== 道具配置 ==========

    @SuppressWarnings("unchecked")
    public List<ItemConfig> findAllConfigs() {
        List<ItemConfig> cached = Cache.get(KEY_CONFIG, List.class);
        if (cached != null) return cached;

        List<ItemConfig> list = DB.execute(ItemConfigMapper.class, ItemConfigMapper::findAllEnabled);
        if (list != null) {
            Cache.set(KEY_CONFIG, list);
        }
        return list;
    }

    public void evictConfigCache() {
        Cache.del(KEY_CONFIG);
    }

    // ========== 玩家道具 ==========

    public List<PlayerItem> findByUserId(long userId) {
        return DB.execute(PlayerItemMapper.class, m -> m.findByUserId(userId));
    }

    public PlayerItem findByUserAndItem(long userId, int itemId) {
        return DB.execute(PlayerItemMapper.class, m -> m.findByUserAndItem(userId, itemId));
    }

    public void insertItem(PlayerItem item) {
        DB.execute(PlayerItemMapper.class, m -> {
            m.insert(item);
            return null;
        });
    }

    public void updateItem(PlayerItem item) {
        DB.execute(PlayerItemMapper.class, m -> {
            m.update(item);
            return null;
        });
    }

    public void deleteItem(long id) {
        DB.execute(PlayerItemMapper.class, m -> {
            m.delete(id);
            return null;
        });
    }
}

