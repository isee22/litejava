package game.account.dao;

import game.account.Cache;
import game.account.DB;
import game.account.entity.GameConfig;
import game.account.mapper.GameConfigMapper;

import java.util.List;

/**
 * 游戏配置 DAO
 */
public class GameConfigDao {

    private static final String KEY_ALL = "game:config:all";
    private static final String KEY = "game:config:";

    @SuppressWarnings("unchecked")
    public List<GameConfig> findAllEnabled() {
        List<GameConfig> cached = Cache.get(KEY_ALL, List.class);
        if (cached != null) return cached;

        List<GameConfig> list = DB.execute(GameConfigMapper.class, GameConfigMapper::findAllEnabled);
        if (list != null) {
            Cache.set(KEY_ALL, list);
        }
        return list;
    }

    public List<GameConfig> findByGameType(String gameType) {
        return DB.execute(GameConfigMapper.class, m -> m.findByGameType(gameType));
    }

    public GameConfig findByTypeAndLevel(String gameType, int roomLevel) {
        String key = KEY + gameType + ":" + roomLevel;
        GameConfig cached = Cache.get(key, GameConfig.class);
        if (cached != null) return cached;

        GameConfig config = DB.execute(GameConfigMapper.class, m -> m.findByTypeAndLevel(gameType, roomLevel));
        if (config != null) {
            Cache.set(key, config);
        }
        return config;
    }

    public void evictCache() {
        Cache.del(KEY_ALL);
    }
}

