package game.doudizhu;

import java.util.*;

/**
 * 斗地主计分器
 */
public class DoudizhuScorer {
    
    // 基础分
    public static final int BASE_SCORE = 100;
    
    public static class ScoreResult {
        public int[] scores;           // 各玩家得分
        public int[] coinChanges;      // 金币变化
        public int winner;             // 赢家座位
        public boolean isLandlord;     // 赢家是否地主
        public int multiple;           // 倍数
        public List<String> details = new ArrayList<>();  // 计分明细
    }
    
    /**
     * 计算得分
     * @param landlordSeat 地主座位
     * @param winnerSeat 赢家座位
     * @param bombCount 炸弹数量
     * @param isSpring 是否春天 (地主出完农民没出 / 农民出完地主只出一手)
     * @param baseMultiple 底分倍数 (叫地主时的倍数)
     */
    public static ScoreResult calculate(int landlordSeat, int winnerSeat, 
                                        int bombCount, boolean isSpring, int baseMultiple) {
        ScoreResult result = new ScoreResult();
        result.scores = new int[3];
        result.coinChanges = new int[3];
        result.winner = winnerSeat;
        result.isLandlord = (winnerSeat == landlordSeat);
        
        // 计算倍数
        int multiple = baseMultiple;
        
        // 炸弹翻倍
        for (int i = 0; i < bombCount; i++) {
            multiple *= 2;
            result.details.add("炸弹 x2");
        }
        
        // 春天翻倍
        if (isSpring) {
            multiple *= 2;
            result.details.add("春天 x2");
        }
        
        result.multiple = multiple;
        
        int score = BASE_SCORE * multiple;
        
        if (result.isLandlord) {
            // 地主赢
            result.scores[landlordSeat] = score * 2;
            result.coinChanges[landlordSeat] = score * 2;
            result.details.add("地主胜利 +" + (score * 2));
            
            for (int i = 0; i < 3; i++) {
                if (i != landlordSeat) {
                    result.scores[i] = -score;
                    result.coinChanges[i] = -score;
                    result.details.add("农民" + (i + 1) + " -" + score);
                }
            }
        } else {
            // 农民赢
            result.scores[landlordSeat] = -score * 2;
            result.coinChanges[landlordSeat] = -score * 2;
            result.details.add("地主失败 -" + (score * 2));
            
            for (int i = 0; i < 3; i++) {
                if (i != landlordSeat) {
                    result.scores[i] = score;
                    result.coinChanges[i] = score;
                    result.details.add("农民" + (i + 1) + " +" + score);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 检查是否春天
     */
    public static boolean checkSpring(int landlordSeat, int[] playedCounts, int winnerSeat) {
        if (winnerSeat == landlordSeat) {
            // 地主赢：检查农民是否都没出过牌
            for (int i = 0; i < 3; i++) {
                if (i != landlordSeat && playedCounts[i] > 0) {
                    return false;
                }
            }
            return true;
        } else {
            // 农民赢：检查地主是否只出过一手牌
            return playedCounts[landlordSeat] <= 1;
        }
    }
}
