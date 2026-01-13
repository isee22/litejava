package game.account.dao;

import game.account.Cache;
import game.account.DB;
import game.account.entity.Player;
import game.account.mapper.PlayerMapper;

import java.util.List;

/**
 * 玩家 DAO
 */
public class PlayerDao {

    private static final String KEY = "player:";

    public Player findById(long userId) {
        Player cached = Cache.get(KEY + userId, Player.class);
        if (cached != null) return cached;

        Player player = DB.execute(PlayerMapper.class, m -> m.findById(userId));
        if (player != null) {
            Cache.set(KEY + userId, player);
        }
        return player;
    }

    public List<Player> findAll() {
        return DB.execute(PlayerMapper.class, PlayerMapper::findAll);
    }

    public void insert(Player player) {
        DB.execute(PlayerMapper.class, m -> {
            m.insert(player);
            return null;
        });
        Cache.set(KEY + player.userId, player);
    }

    public void update(Player player) {
        DB.execute(PlayerMapper.class, m -> {
            m.update(player);
            return null;
        });
        Cache.set(KEY + player.userId, player);
    }

    public void evict(long userId) {
        Cache.del(KEY + userId);
    }
}

