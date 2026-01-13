package game.niuniu;

import java.util.*;

/**
 * 牛牛计分器
 */
public class NiuniuScorer {
    
    // 牛型倍数
    public static final int[] NIU_MULTIPLES = {
        1,  // 没牛
        1,  // 牛一
        1,  // 牛二
        1,  // 牛三
        1,  // 牛四
        1,  // 牛五
        1,  // 牛六
        2,  // 牛七
        2,  // 牛八
        3,  // 牛九
        3,  // 牛牛
    };
    
    public static class ScoreResult {
        public int[] scores;           // 各玩家得分
        public int[] niuTypes;         // 各玩家牛型
        public int bankerSeat;         // 庄家座位
        public int baseScore;          // 底分
        public List<String> details = new ArrayList<>();
    }
    
    /**
     * 计算结算
     * @param bankerSeat 庄家座位
     * @param cards 各玩家手牌 (5张)
     * @param bets 各玩家下注倍数
     * @param baseScore 底分
     */
    public static ScoreResult calculate(int bankerSeat, int[][] cards, int[] bets, int baseScore) {
        int n = cards.length;
        ScoreResult result = new ScoreResult();
        result.scores = new int[n];
        result.niuTypes = new int[n];
        result.bankerSeat = bankerSeat;
        result.baseScore = baseScore;
        
        // 计算各玩家牛型
        for (int i = 0; i < n; i++) {
            result.niuTypes[i] = NiuniuAI.calculateNiu(cards[i]);
        }
        
        int bankerNiu = result.niuTypes[bankerSeat];
        int bankerMultiple = NIU_MULTIPLES[bankerNiu];
        
        // 庄家与每个闲家比较
        for (int i = 0; i < n; i++) {
            if (i == bankerSeat) continue;
            
            int playerNiu = result.niuTypes[i];
            int playerMultiple = NIU_MULTIPLES[playerNiu];
            int betMultiple = bets[i];
            
            int cmp = NiuniuAI.compare(cards[bankerSeat], cards[i]);
            
            if (cmp > 0) {
                // 庄家赢
                int win = baseScore * betMultiple * bankerMultiple;
                result.scores[bankerSeat] += win;
                result.scores[i] -= win;
                result.details.add("庄家 vs 玩家" + i + ": 庄赢 +" + win);
            } else {
                // 闲家赢
                int win = baseScore * betMultiple * playerMultiple;
                result.scores[bankerSeat] -= win;
                result.scores[i] += win;
                result.details.add("庄家 vs 玩家" + i + ": 闲赢 +" + win);
            }
        }
        
        return result;
    }
    
    /**
     * 获取牛型名称
     */
    public static String getNiuName(int niu) {
        switch (niu) {
            case 0: return "没牛";
            case 1: return "牛一";
            case 2: return "牛二";
            case 3: return "牛三";
            case 4: return "牛四";
            case 5: return "牛五";
            case 6: return "牛六";
            case 7: return "牛七";
            case 8: return "牛八";
            case 9: return "牛九";
            case 10: return "牛牛";
            default: return "未知";
        }
    }
}
