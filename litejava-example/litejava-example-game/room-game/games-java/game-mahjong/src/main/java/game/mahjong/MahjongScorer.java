package game.mahjong;

import java.util.*;

/**
 * 麻将计分器 (四川麻将)
 */
public class MahjongScorer {
    
    // 基础番数
    public static final int BASE_FAN = 1;
    
    public static class ScoreResult {
        public int[] scores;           // 各玩家得分
        public int winner;             // 赢家
        public int loser;              // 点炮者 (-1 表示自摸)
        public int fan;                // 番数
        public int baseScore;          // 底分
        public List<String> fanTypes = new ArrayList<>();  // 番型
    }
    
    /**
     * 计算得分
     * @param winnerSeat 赢家座位
     * @param loserSeat 点炮者座位 (-1 表示自摸)
     * @param hand 赢家手牌
     * @param melds 赢家副露
     * @param winCard 胡的牌
     * @param isSelfDrawn 是否自摸
     * @param baseScore 底分
     */
    public static ScoreResult calculate(int winnerSeat, int loserSeat, 
                                        List<Integer> hand, List<int[]> melds,
                                        int winCard, boolean isSelfDrawn, int baseScore) {
        ScoreResult result = new ScoreResult();
        result.scores = new int[4];
        result.winner = winnerSeat;
        result.loser = loserSeat;
        result.baseScore = baseScore;
        
        // 计算番数
        int fan = BASE_FAN;
        
        // 自摸 +1番
        if (isSelfDrawn) {
            fan++;
            result.fanTypes.add("自摸");
        }
        
        // 检查特殊番型
        if (isQingYiSe(hand, melds)) {
            fan += 2;
            result.fanTypes.add("清一色");
        }
        
        if (isDuiDuiHu(hand, melds)) {
            fan += 1;
            result.fanTypes.add("对对胡");
        }
        
        if (isQiDui(hand)) {
            fan += 2;
            result.fanTypes.add("七对");
        }
        
        // 杠上开花
        // 抢杠胡
        // 等其他番型...
        
        result.fan = fan;
        
        // 计算分数
        int score = baseScore * (1 << fan);  // 2^fan
        
        if (isSelfDrawn) {
            // 自摸：其他三家各出
            for (int i = 0; i < 4; i++) {
                if (i == winnerSeat) {
                    result.scores[i] = score * 3;
                } else {
                    result.scores[i] = -score;
                }
            }
        } else {
            // 点炮：点炮者出
            result.scores[winnerSeat] = score;
            result.scores[loserSeat] = -score;
        }
        
        return result;
    }
    
    /**
     * 清一色：全部同一花色
     */
    private static boolean isQingYiSe(List<Integer> hand, List<int[]> melds) {
        Set<Integer> types = new HashSet<>();
        
        for (int card : hand) {
            types.add(card / 10);
        }
        
        if (melds != null) {
            for (int[] meld : melds) {
                for (int card : meld) {
                    types.add(card / 10);
                }
            }
        }
        
        return types.size() == 1;
    }
    
    /**
     * 对对胡：全部刻子
     */
    private static boolean isDuiDuiHu(List<Integer> hand, List<int[]> melds) {
        // 检查手牌是否全是刻子+将
        Map<Integer, Integer> counts = new HashMap<>();
        for (int card : hand) {
            counts.merge(card, 1, Integer::sum);
        }
        
        int pairs = 0;
        for (int count : counts.values()) {
            if (count == 2) pairs++;
            else if (count != 3) return false;
        }
        
        return pairs == 1;
    }
    
    /**
     * 七对：7个对子
     */
    private static boolean isQiDui(List<Integer> hand) {
        if (hand.size() != 14) return false;
        
        Map<Integer, Integer> counts = new HashMap<>();
        for (int card : hand) {
            counts.merge(card, 1, Integer::sum);
        }
        
        for (int count : counts.values()) {
            if (count != 2 && count != 4) return false;
        }
        
        return counts.size() == 7 || (counts.size() < 7 && counts.values().stream().anyMatch(c -> c == 4));
    }
}
