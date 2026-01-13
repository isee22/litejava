package game.mahjong;

import java.util.*;

/**
 * 麻将 AI (四川麻将)
 * 
 * 策略：
 * - 打生张、留熟张
 * - 优先保留搭子
 * - 能碰就碰、能杠就杠
 */
public class MahjongAI {
    
    /**
     * 选择要打的牌
     * @param hand 手牌
     * @param discards 已打出的牌 (用于判断生熟张)
     * @return 要打的牌
     */
    public static int selectDiscard(List<Integer> hand, List<Integer> discards) {
        if (hand == null || hand.isEmpty()) return -1;
        
        // 统计每张牌的数量
        Map<Integer, Integer> counts = new HashMap<>();
        for (int card : hand) {
            counts.merge(card, 1, Integer::sum);
        }
        
        // 统计已打出的牌
        Set<Integer> discardSet = new HashSet<>(discards != null ? discards : Collections.emptyList());
        
        int bestCard = hand.get(0);
        int bestScore = Integer.MAX_VALUE;
        
        for (int card : hand) {
            int score = evaluateCard(card, counts, discardSet);
            if (score < bestScore) {
                bestScore = score;
                bestCard = card;
            }
        }
        
        return bestCard;
    }
    
    /**
     * 是否应该碰
     */
    public static boolean shouldPeng(List<Integer> hand, int card) {
        int count = 0;
        for (int c : hand) {
            if (c == card) count++;
        }
        return count >= 2;
    }
    
    /**
     * 是否应该杠
     */
    public static boolean shouldGang(List<Integer> hand, int card, boolean isSelf) {
        int count = 0;
        for (int c : hand) {
            if (c == card) count++;
        }
        // 暗杠需要4张，明杠需要3张(已碰)+1张
        return isSelf ? count >= 4 : count >= 1;
    }
    
    /**
     * 检查是否能胡
     */
    public static boolean canHu(List<Integer> hand) {
        if (hand.size() % 3 != 2) return false;
        return checkHu(new ArrayList<>(hand), false);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 评估单张牌的价值 (越低越应该打出)
     */
    private static int evaluateCard(int card, Map<Integer, Integer> counts, Set<Integer> discards) {
        int score = 0;
        int count = counts.getOrDefault(card, 0);
        
        // 对子/刻子价值高
        if (count >= 3) score += 100;
        else if (count >= 2) score += 50;
        
        // 搭子价值
        int type = card / 10;  // 1=万, 2=条, 3=筒
        int value = card % 10;
        
        if (type >= 1 && type <= 3 && value >= 1 && value <= 9) {
            // 检查顺子搭子
            if (value > 1 && counts.containsKey(card - 1)) score += 30;
            if (value < 9 && counts.containsKey(card + 1)) score += 30;
            if (value > 2 && counts.containsKey(card - 2)) score += 15;
            if (value < 8 && counts.containsKey(card + 2)) score += 15;
            
            // 边张价值低
            if (value == 1 || value == 9) score -= 10;
        }
        
        // 生张价值低 (没人打过的牌)
        if (!discards.contains(card)) score -= 20;
        
        return score;
    }
    
    /**
     * 递归检查是否能胡
     */
    private static boolean checkHu(List<Integer> hand, boolean hasJiang) {
        if (hand.isEmpty()) return hasJiang;
        
        Collections.sort(hand);
        int first = hand.get(0);
        
        // 尝试作为将
        if (!hasJiang) {
            int count = 0;
            for (int c : hand) if (c == first) count++;
            if (count >= 2) {
                List<Integer> remain = new ArrayList<>(hand);
                remain.remove(Integer.valueOf(first));
                remain.remove(Integer.valueOf(first));
                if (checkHu(remain, true)) return true;
            }
        }
        
        // 尝试作为刻子
        int count = 0;
        for (int c : hand) if (c == first) count++;
        if (count >= 3) {
            List<Integer> remain = new ArrayList<>(hand);
            remain.remove(Integer.valueOf(first));
            remain.remove(Integer.valueOf(first));
            remain.remove(Integer.valueOf(first));
            if (checkHu(remain, hasJiang)) return true;
        }
        
        // 尝试作为顺子
        int type = first / 10;
        int value = first % 10;
        if (type >= 1 && type <= 3 && value <= 7) {
            int second = first + 1;
            int third = first + 2;
            if (hand.contains(second) && hand.contains(third)) {
                List<Integer> remain = new ArrayList<>(hand);
                remain.remove(Integer.valueOf(first));
                remain.remove(Integer.valueOf(second));
                remain.remove(Integer.valueOf(third));
                if (checkHu(remain, hasJiang)) return true;
            }
        }
        
        return false;
    }
}
