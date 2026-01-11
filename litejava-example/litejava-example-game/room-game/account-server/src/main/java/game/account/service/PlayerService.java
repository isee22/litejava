package game.account.service;

import game.account.DB;
import game.account.entity.PlayerEntity;
import game.account.mapper.PlayerMapper;

import java.util.*;

/**
 * 玩家服务
 */
public class PlayerService {
    
    /**
     * 获取玩家，不存在则创建
     */
    public static PlayerEntity getOrCreate(long userId, String name) {
        PlayerEntity player = DB.execute(PlayerMapper.class, m -> m.findById(userId));
        if (player != null) {
            return player;
        }
        
        // 创建新玩家
        player = new PlayerEntity();
        player.userId = userId;
        player.name = name;
        player.sex = 0;       // 未知
        player.coins = 10000;  // 初始金币
        player.diamonds = 100; // 初始钻石
        player.level = 1;
        player.exp = 0;
        player.vipLevel = 0;
        player.vipExp = 0;
        player.createTime = System.currentTimeMillis();
        player.lastLoginTime = System.currentTimeMillis();
        player.loginDays = 1;
        player.totalGames = 0;
        player.winGames = 0;
        
        PlayerEntity finalPlayer = player;
        DB.execute(PlayerMapper.class, m -> {
            m.insert(finalPlayer);
            return null;
        });
        
        return player;
    }
    
    /**
     * 获取玩家
     */
    public static PlayerEntity get(long userId) {
        return DB.execute(PlayerMapper.class, m -> m.findById(userId));
    }
    
    /**
     * 更新登录信息
     */
    public static void updateLogin(long userId) {
        PlayerEntity player = get(userId);
        if (player == null) return;
        
        long now = System.currentTimeMillis();
        long lastLogin = player.lastLoginTime;
        
        // 判断是否新的一天
        boolean isNewDay = !isSameDay(lastLogin, now);
        if (isNewDay) {
            player.loginDays++;
        }
        
        player.lastLoginTime = now;
        DB.execute(PlayerMapper.class, m -> {
            m.update(player);
            return null;
        });
    }
    
    /**
     * 增加金币
     */
    public static boolean addCoins(long userId, int amount, String reason) {
        PlayerEntity player = get(userId);
        if (player == null) return false;
        
        player.coins += amount;
        if (player.coins < 0) player.coins = 0;
        
        DB.execute(PlayerMapper.class, m -> {
            m.update(player);
            return null;
        });
        return true;
    }
    
    /**
     * 增加钻石
     */
    public static boolean addDiamonds(long userId, int amount, String reason) {
        PlayerEntity player = get(userId);
        if (player == null) return false;
        
        player.diamonds += amount;
        if (player.diamonds < 0) player.diamonds = 0;
        
        DB.execute(PlayerMapper.class, m -> {
            m.update(player);
            return null;
        });
        return true;
    }
    
    /**
     * 增加经验
     */
    public static void addExp(long userId, int exp) {
        PlayerEntity player = get(userId);
        if (player == null) return;
        
        player.exp += exp;
        
        // 升级检查
        while (player.exp >= getExpForLevel(player.level + 1)) {
            player.exp -= getExpForLevel(player.level + 1);
            player.level++;
        }
        
        DB.execute(PlayerMapper.class, m -> {
            m.update(player);
            return null;
        });
    }
    
    /**
     * 记录游戏结果
     */
    public static void recordGame(long userId, boolean win) {
        PlayerEntity player = get(userId);
        if (player == null) return;
        
        player.totalGames++;
        if (win) player.winGames++;
        
        DB.execute(PlayerMapper.class, m -> {
            m.update(player);
            return null;
        });
    }
    
    /**
     * 获取升级所需经验
     */
    public static int getExpForLevel(int level) {
        return level * 100;
    }
    
    private static boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) 
            && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
