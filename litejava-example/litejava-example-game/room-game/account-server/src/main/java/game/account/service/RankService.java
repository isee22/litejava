package game.account.service;

import game.account.DB;
import game.account.entity.PlayerEntity;
import game.account.mapper.PlayerMapper;
import game.account.vo.CharmRankVO;
import game.account.vo.RankItemVO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 排行榜服务
 */
public class RankService {
    
    // 排行榜类型
    public static final String RANK_COINS = "coins";      // 金币榜
    public static final String RANK_WINS = "wins";        // 胜场榜
    public static final String RANK_LEVEL = "level";      // 等级榜
    public static final String RANK_CHARM = "charm";      // 魅力榜
    
    // 缓存 (定期刷新)
    private static final Map<String, List<RankItemVO>> rankCache = new ConcurrentHashMap<>();
    private static long lastRefreshTime = 0;
    private static final long REFRESH_INTERVAL = 60000; // 1分钟刷新一次
    
    /**
     * 获取排行榜
     */
    public static List<RankItemVO> getRank(String type, int limit) {
        refreshIfNeeded();
        
        List<RankItemVO> rank = rankCache.get(type);
        if (rank == null) {
            return Collections.emptyList();
        }
        
        return rank.subList(0, Math.min(limit, rank.size()));
    }
    
    /**
     * 获取玩家排名
     */
    public static int getPlayerRank(String type, long userId) {
        refreshIfNeeded();
        
        List<RankItemVO> rank = rankCache.get(type);
        if (rank == null) return -1;
        
        for (int i = 0; i < rank.size(); i++) {
            if (rank.get(i).userId == userId) {
                return i + 1;
            }
        }
        return -1;
    }
    
    /**
     * 刷新排行榜
     */
    private static synchronized void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < REFRESH_INTERVAL) {
            return;
        }
        lastRefreshTime = now;
        
        // 获取所有玩家 (实际应该分页或只取TOP N)
        List<PlayerEntity> players = DB.execute(PlayerMapper.class, PlayerMapper::findAll);
        if (players == null) {
            players = Collections.emptyList();
        }
        
        // 金币榜
        List<PlayerEntity> byCoins = new ArrayList<>(players);
        byCoins.sort((a, b) -> b.coins - a.coins);
        rankCache.put(RANK_COINS, toRankList(byCoins, "coins"));
        
        // 胜场榜
        List<PlayerEntity> byWins = new ArrayList<>(players);
        byWins.sort((a, b) -> b.winGames - a.winGames);
        rankCache.put(RANK_WINS, toRankList(byWins, "wins"));
        
        // 等级榜
        List<PlayerEntity> byLevel = new ArrayList<>(players);
        byLevel.sort((a, b) -> {
            if (b.level != a.level) return b.level - a.level;
            return b.exp - a.exp;
        });
        rankCache.put(RANK_LEVEL, toRankList(byLevel, "level"));
        
        // 魅力榜 (从 GiftService 获取)
        List<CharmRankVO> charmRank = GiftService.getCharmRank(100);
        List<RankItemVO> charmItems = new ArrayList<>();
        for (CharmRankVO cr : charmRank) {
            RankItemVO item = new RankItemVO();
            item.rank = cr.rank;
            item.userId = cr.userId;
            item.name = cr.name;
            item.charm = cr.charm;
            item.value = cr.charm;
            charmItems.add(item);
        }
        rankCache.put(RANK_CHARM, charmItems);
    }
    
    private static List<RankItemVO> toRankList(List<PlayerEntity> players, String valueField) {
        List<RankItemVO> result = new ArrayList<>();
        int limit = Math.min(100, players.size());
        
        for (int i = 0; i < limit; i++) {
            PlayerEntity p = players.get(i);
            RankItemVO item = new RankItemVO();
            item.rank = i + 1;
            item.userId = p.userId;
            item.name = p.name;
            item.level = p.level;
            
            switch (valueField) {
                case "coins":
                    item.value = p.coins;
                    break;
                case "wins":
                    item.value = p.winGames;
                    item.total = p.totalGames;
                    break;
                case "level":
                    item.value = p.level;
                    item.exp = p.exp;
                    break;
            }
            
            result.add(item);
        }
        return result;
    }
}
