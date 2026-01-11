package game.doudizhu;

import java.util.*;

/**
 * 斗地主 AI
 * 
 * 简单策略：
 * - 叫地主：手牌好就叫
 * - 出牌：能出就出最小的
 */
public class DoudizhuAI {
    
    /**
     * 决定是否叫地主
     */
    public static boolean shouldBid(int[] cards) {
        // 简单策略：有大小王或炸弹就叫
        int kings = 0;
        Map<Integer, Integer> counts = new HashMap<>();
        
        for (int card : cards) {
            int value = card % 100;
            if (value >= 16) kings++;  // 大小王
            counts.merge(value, 1, Integer::sum);
        }
        
        // 有炸弹
        for (int count : counts.values()) {
            if (count >= 4) return true;
        }
        
        return kings >= 1;
    }
    
    /**
     * 选择要出的牌
     * @param hand 手牌
     * @param lastCards 上家出的牌 (null 表示自己先出)
     * @return 要出的牌，null 表示不出
     */
    public static int[] selectCards(int[] hand, int[] lastCards) {
        if (hand == null || hand.length == 0) return null;
        
        // 自己先出：出最小的单张
        if (lastCards == null || lastCards.length == 0) {
            int min = findMinCard(hand);
            return new int[]{min};
        }
        
        // 跟牌
        int lastType = getCardType(lastCards);
        int lastValue = getCardValue(lastCards);
        
        switch (lastType) {
            case 1:  // 单张
                return findSingle(hand, lastValue);
            case 2:  // 对子
                return findPair(hand, lastValue);
            case 3:  // 三张
                return findTriple(hand, lastValue);
            case 4:  // 炸弹
                return findBomb(hand, lastValue);
            default:
                return null;  // 复杂牌型暂不处理
        }
    }
    
    /**
     * 是否应该不出
     */
    public static boolean shouldPass(int[] hand, int[] lastCards, boolean isTeammate) {
        if (lastCards == null || lastCards.length == 0) return false;
        if (isTeammate && lastCards.length >= 3) return true;  // 队友大牌不压
        
        int[] canPlay = selectCards(hand, lastCards);
        return canPlay == null;
    }
    
    // ==================== 辅助方法 ====================
    
    private static int findMinCard(int[] cards) {
        int min = cards[0];
        for (int card : cards) {
            if ((card % 100) < (min % 100)) {
                min = card;
            }
        }
        return min;
    }
    
    private static int[] findSingle(int[] hand, int minValue) {
        int best = -1;
        for (int card : hand) {
            int value = card % 100;
            if (value > minValue) {
                if (best < 0 || value < (best % 100)) {
                    best = card;
                }
            }
        }
        return best >= 0 ? new int[]{best} : null;
    }
    
    private static int[] findPair(int[] hand, int minValue) {
        Map<Integer, List<Integer>> groups = groupByValue(hand);
        
        int bestValue = -1;
        for (Map.Entry<Integer, List<Integer>> e : groups.entrySet()) {
            if (e.getValue().size() >= 2 && e.getKey() > minValue) {
                if (bestValue < 0 || e.getKey() < bestValue) {
                    bestValue = e.getKey();
                }
            }
        }
        
        if (bestValue >= 0) {
            List<Integer> cards = groups.get(bestValue);
            return new int[]{cards.get(0), cards.get(1)};
        }
        return null;
    }
    
    private static int[] findTriple(int[] hand, int minValue) {
        Map<Integer, List<Integer>> groups = groupByValue(hand);
        
        int bestValue = -1;
        for (Map.Entry<Integer, List<Integer>> e : groups.entrySet()) {
            if (e.getValue().size() >= 3 && e.getKey() > minValue) {
                if (bestValue < 0 || e.getKey() < bestValue) {
                    bestValue = e.getKey();
                }
            }
        }
        
        if (bestValue >= 0) {
            List<Integer> cards = groups.get(bestValue);
            return new int[]{cards.get(0), cards.get(1), cards.get(2)};
        }
        return null;
    }
    
    private static int[] findBomb(int[] hand, int minValue) {
        Map<Integer, List<Integer>> groups = groupByValue(hand);
        
        int bestValue = -1;
        for (Map.Entry<Integer, List<Integer>> e : groups.entrySet()) {
            if (e.getValue().size() >= 4 && e.getKey() > minValue) {
                if (bestValue < 0 || e.getKey() < bestValue) {
                    bestValue = e.getKey();
                }
            }
        }
        
        if (bestValue >= 0) {
            List<Integer> cards = groups.get(bestValue);
            return new int[]{cards.get(0), cards.get(1), cards.get(2), cards.get(3)};
        }
        
        // 王炸
        boolean hasSmallKing = false, hasBigKing = false;
        int smallKing = -1, bigKing = -1;
        for (int card : hand) {
            int value = card % 100;
            if (value == 16) { hasSmallKing = true; smallKing = card; }
            if (value == 17) { hasBigKing = true; bigKing = card; }
        }
        if (hasSmallKing && hasBigKing) {
            return new int[]{smallKing, bigKing};
        }
        
        return null;
    }
    
    private static Map<Integer, List<Integer>> groupByValue(int[] cards) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int card : cards) {
            int value = card % 100;
            groups.computeIfAbsent(value, k -> new ArrayList<>()).add(card);
        }
        return groups;
    }
    
    private static int getCardType(int[] cards) {
        if (cards.length == 1) return 1;  // 单张
        if (cards.length == 2) {
            int v1 = cards[0] % 100, v2 = cards[1] % 100;
            if (v1 == v2) return 2;  // 对子
            if ((v1 == 16 && v2 == 17) || (v1 == 17 && v2 == 16)) return 4;  // 王炸
        }
        if (cards.length == 3) {
            int v1 = cards[0] % 100, v2 = cards[1] % 100, v3 = cards[2] % 100;
            if (v1 == v2 && v2 == v3) return 3;  // 三张
        }
        if (cards.length == 4) {
            int v1 = cards[0] % 100;
            boolean same = true;
            for (int card : cards) {
                if (card % 100 != v1) { same = false; break; }
            }
            if (same) return 4;  // 炸弹
        }
        return 0;  // 其他
    }
    
    private static int getCardValue(int[] cards) {
        if (cards.length == 0) return 0;
        // 王炸最大
        if (cards.length == 2) {
            int v1 = cards[0] % 100, v2 = cards[1] % 100;
            if ((v1 == 16 && v2 == 17) || (v1 == 17 && v2 == 16)) return 100;
        }
        return cards[0] % 100;
    }
}
