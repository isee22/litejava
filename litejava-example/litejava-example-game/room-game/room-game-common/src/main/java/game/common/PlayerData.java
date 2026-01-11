package game.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据管理 (内存版)
 */
public class PlayerData {
    
    private static final Map<Long, Player> players = new ConcurrentHashMap<>();
    
    public static class Player {
        public long userId;
        public String name;
        public String avatar;
        public int coins;
        public int diamonds;
        public int level;
        public int exp;
        public long createTime;
        public long lastLoginTime;
        public Map<String, GameStats> gameStats = new HashMap<>();
    }
    
    public static class GameStats {
        public String gameType;
        public int totalGames;
        public int wins;
        public int losses;
        public int draws;
        public int maxWinStreak;
        public int curWinStreak;
        public long totalScore;
    }
    
    public static Player get(long userId) {
        return players.get(userId);
    }
    
    public static Player getOrCreate(long userId, String name) {
        return players.computeIfAbsent(userId, id -> {
            Player p = new Player();
            p.userId = id;
            p.name = name;
            p.coins = 1000;
            p.diamonds = 10;
            p.level = 1;
            p.createTime = System.currentTimeMillis();
            p.lastLoginTime = p.createTime;
            return p;
        });
    }
    
    public static void updateLogin(long userId) {
        Player p = players.get(userId);
        if (p != null) {
            p.lastLoginTime = System.currentTimeMillis();
        }
    }
    
    public static boolean addCoins(long userId, int amount) {
        Player p = players.get(userId);
        if (p == null) return false;
        if (amount < 0 && p.coins + amount < 0) return false;
        p.coins += amount;
        return true;
    }
    
    public static boolean addDiamonds(long userId, int amount) {
        Player p = players.get(userId);
        if (p == null) return false;
        if (amount < 0 && p.diamonds + amount < 0) return false;
        p.diamonds += amount;
        return true;
    }
    
    public static void recordGame(long userId, String gameType, boolean win, int score) {
        Player p = players.get(userId);
        if (p == null) return;
        
        GameStats stats = p.gameStats.computeIfAbsent(gameType, k -> {
            GameStats s = new GameStats();
            s.gameType = k;
            return s;
        });
        
        stats.totalGames++;
        stats.totalScore += score;
        
        if (win) {
            stats.wins++;
            stats.curWinStreak++;
            if (stats.curWinStreak > stats.maxWinStreak) {
                stats.maxWinStreak = stats.curWinStreak;
            }
        } else {
            stats.losses++;
            stats.curWinStreak = 0;
        }
        
        p.exp += Math.max(10, score / 10);
        int needExp = p.level * 100;
        while (p.exp >= needExp) {
            p.exp -= needExp;
            p.level++;
            needExp = p.level * 100;
        }
    }
    
    public static GameStats getStats(long userId, String gameType) {
        Player p = players.get(userId);
        if (p == null) return null;
        return p.gameStats.get(gameType);
    }
}
