package game.account.service;

import game.account.Cache;
import game.account.DB;
import game.account.entity.GameConfig;
import game.account.mapper.GameConfigMapper;

import java.util.List;

/**
 * 配置服务
 * 
 * 管理游戏配置，并同步到缓存供 HallServer 读取
 * 
 * 缓存 Key:
 * - hall:configs (所有配置列表 JSON)
 * - hall:config:{gameType}:{roomLevel} (单个配置)
 */
public class ConfigService {

    private static final String KEY_CONFIGS = "hall:configs";
    private static final String KEY_PREFIX = "hall:config:";

    /**
     * 获取所有启用的游戏配置
     */
    public List<GameConfig> findAllEnabled() {
        return DB.execute(GameConfigMapper.class, GameConfigMapper::findAllEnabled);
    }

    /**
     * 获取某游戏的所有场次
     */
    public List<GameConfig> findByGameType(String gameType) {
        return DB.execute(GameConfigMapper.class, m -> m.findByGameType(gameType));
    }

    /**
     * 获取某游戏某场次配置
     */
    public GameConfig findByTypeAndLevel(String gameType, int roomLevel) {
        return DB.execute(GameConfigMapper.class, m -> m.findByTypeAndLevel(gameType, roomLevel));
    }

    /**
     * 同步所有启用的游戏配置到缓存
     */
    public void syncAll() {
        List<GameConfig> configs = findAllEnabled();
        if (configs == null) return;

        // 存储配置列表
        Cache.set(KEY_CONFIGS, configs, 0);
        
        // 同时存储单个配置 (方便按 key 查询)
        for (GameConfig config : configs) {
            syncOne(config);
        }
    }

    /**
     * 同步单个配置到缓存
     */
    public void syncOne(GameConfig config) {
        if (config == null) return;
        String key = KEY_PREFIX + config.gameType + ":" + config.roomLevel;
        Cache.set(key, config, 0); // 永不过期
    }

    /**
     * 删除缓存中的配置
     */
    public void remove(String gameType, int roomLevel) {
        Cache.del(KEY_PREFIX + gameType + ":" + roomLevel);
    }

    /**
     * 刷新所有配置到缓存
     */
    public void refresh() {
        syncAll();
    }
}

