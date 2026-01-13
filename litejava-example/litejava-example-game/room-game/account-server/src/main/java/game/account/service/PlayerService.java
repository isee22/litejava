package game.account.service;

import game.account.dao.PlayerDao;
import game.account.entity.Player;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * 玩家服务
 */
public class PlayerService {

    private final PlayerDao playerDao = new PlayerDao();

    public Player get(long userId) {
        return playerDao.findById(userId);
    }

    public Player getOrCreate(long userId, String name) {
        Player player = playerDao.findById(userId);
        if (player != null) return player;

        player = new Player();
        player.userId = userId;
        player.name = name;
        player.sex = 0;
        player.coins = 10000;
        player.diamonds = 100;
        player.level = 1;
        player.exp = 0;
        player.vipLevel = 0;
        player.vipExp = 0;
        player.createTime = System.currentTimeMillis();
        player.lastLoginTime = System.currentTimeMillis();
        player.loginDays = 1;
        player.totalGames = 0;
        player.winGames = 0;
        player.escapeGames = 0;
        player.creditScore = 100;  // 初始信用分

        playerDao.insert(player);
        return player;
    }

    public void updateLogin(long userId) {
        Player player = playerDao.findById(userId);
        if (player == null) return;

        long now = System.currentTimeMillis();
        if (!isSameDay(player.lastLoginTime, now)) {
            player.loginDays++;
        }
        player.lastLoginTime = now;
        playerDao.update(player);
    }

    public boolean addCoins(long userId, int amount, String reason) {
        Player player = playerDao.findById(userId);
        if (player == null) return false;

        player.coins = Math.max(0, player.coins + amount);
        playerDao.update(player);
        return true;
    }

    public boolean addDiamonds(long userId, int amount, String reason) {
        Player player = playerDao.findById(userId);
        if (player == null) return false;

        player.diamonds = Math.max(0, player.diamonds + amount);
        playerDao.update(player);
        return true;
    }

    public void addExp(long userId, int exp) {
        Player player = playerDao.findById(userId);
        if (player == null) return;

        player.exp += exp;
        while (player.exp >= getExpForLevel(player.level + 1)) {
            player.exp -= getExpForLevel(player.level + 1);
            player.level++;
        }
        playerDao.update(player);
    }

    public void recordGame(long userId, boolean win) {
        Player player = playerDao.findById(userId);
        if (player == null) return;

        player.totalGames++;
        if (win) player.winGames++;
        playerDao.update(player);
    }
    
    /**
     * 批量结算 (GameServer 调用)
     * 
     * @param settlements 结算列表 [{userId, win, score, coinChange}]
     */
    public void batchSettle(List<Map<String, Object>> settlements) {
        for (Map<String, Object> s : settlements) {
            long userId = ((Number) s.get("userId")).longValue();
            boolean win = Boolean.TRUE.equals(s.get("win"));
            int coinChange = s.get("coinChange") != null ? ((Number) s.get("coinChange")).intValue() : 0;
            int exp = s.get("exp") != null ? ((Number) s.get("exp")).intValue() : 10;
            
            Player player = playerDao.findById(userId);
            if (player == null) continue;
            
            player.totalGames++;
            if (win) player.winGames++;
            player.coins = Math.max(0, player.coins + coinChange);
            player.exp += exp;
            
            while (player.exp >= getExpForLevel(player.level + 1)) {
                player.exp -= getExpForLevel(player.level + 1);
                player.level++;
            }
            
            playerDao.update(player);
        }
    }
    
    /**
     * 记录逃跑
     * 
     * 逃跑次数+1，信用分-5
     */
    public void recordEscape(long userId) {
        Player player = playerDao.findById(userId);
        if (player == null) return;
        
        player.escapeGames++;
        player.creditScore = Math.max(0, player.creditScore - 5);
        playerDao.update(player);
    }

    public int getExpForLevel(int level) {
        return level * 100;
    }

    private boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(t1);
        c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}

