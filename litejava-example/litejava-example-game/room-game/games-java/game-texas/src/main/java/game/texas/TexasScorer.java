package game.texas;

import java.util.*;

/**
 * 德州扑克计分器
 */
public class TexasScorer {
    
    // 牌型等级
    public static final int HIGH_CARD = 1;
    public static final int ONE_PAIR = 2;
    public static final int TWO_PAIR = 3;
    public static final int THREE_OF_KIND = 4;
    public static final int STRAIGHT = 5;
    public static final int FLUSH = 6;
    public static final int FULL_HOUSE = 7;
    public static final int FOUR_OF_KIND = 8;
    public static final int STRAIGHT_FLUSH = 9;
    public static final int ROYAL_FLUSH = 10;
    
    public static class HandRank {
        public int rank;           // 牌型等级
        public String rankName;    // 牌型名称
        public int[] kickers;      // 比较用的牌值
    }
    
    /**
     * 评估最佳5张牌
     * @param holeCards 手牌 (2张)
     * @param communityCards 公共牌 (3-5张)
     */
    public static HandRank evaluate(int[] holeCards, int[] communityCards) {
        int[] allCards = new int[holeCards.length + communityCards.length];
        System.arraycopy(holeCards, 0, allCards, 0, holeCards.length);
        System.arraycopy(communityCards, 0, allCards, holeCards.length, communityCards.length);
        
        return evaluateBest5(allCards);
    }
    
    /**
     * 比较两手牌
     * @return >0 表示 hand1 大, <0 表示 hand2 大, 0 表示相等
     */
    public static int compare(HandRank hand1, HandRank hand2) {
        if (hand1.rank != hand2.rank) {
            return hand1.rank - hand2.rank;
        }
        
        for (int i = 0; i < Math.min(hand1.kickers.length, hand2.kickers.length); i++) {
            if (hand1.kickers[i] != hand2.kickers[i]) {
                return hand1.kickers[i] - hand2.kickers[i];
            }
        }
        
        return 0;
    }
    
    /**
     * 计算边池分配
     */
    public static int[] calculatePots(int[] bets, boolean[] folded, HandRank[] ranks) {
        int n = bets.length;
        int[] winnings = new int[n];
        
        // 简化：找出最大牌型的玩家
        int bestIdx = -1;
        HandRank bestRank = null;
        
        for (int i = 0; i < n; i++) {
            if (folded[i]) continue;
            if (bestRank == null || compare(ranks[i], bestRank) > 0) {
                bestRank = ranks[i];
                bestIdx = i;
            }
        }
        
        // 赢家获得所有筹码
        if (bestIdx >= 0) {
            int pot = 0;
            for (int bet : bets) pot += bet;
            winnings[bestIdx] = pot;
        }
        
        return winnings;
    }
    
    // ==================== 内部方法 ====================
    
    private static HandRank evaluateBest5(int[] cards) {
        HandRank best = null;
        
        // 枚举所有5张牌组合
        int n = cards.length;
        for (int i = 0; i < n - 4; i++) {
            for (int j = i + 1; j < n - 3; j++) {
                for (int k = j + 1; k < n - 2; k++) {
                    for (int l = k + 1; l < n - 1; l++) {
                        for (int m = l + 1; m < n; m++) {
                            int[] five = {cards[i], cards[j], cards[k], cards[l], cards[m]};
                            HandRank rank = evaluate5(five);
                            if (best == null || compare(rank, best) > 0) {
                                best = rank;
                            }
                        }
                    }
                }
            }
        }
        
        return best;
    }
    
    private static HandRank evaluate5(int[] cards) {
        int[] values = new int[5];
        int[] suits = new int[5];
        
        for (int i = 0; i < 5; i++) {
            values[i] = cards[i] % 13;
            suits[i] = cards[i] / 13;
        }
        
        Arrays.sort(values);
        
        boolean isFlush = suits[0] == suits[1] && suits[1] == suits[2] 
                       && suits[2] == suits[3] && suits[3] == suits[4];
        
        boolean isStraight = isStraight(values);
        
        Map<Integer, Integer> counts = new HashMap<>();
        for (int v : values) {
            counts.merge(v, 1, Integer::sum);
        }
        
        List<Integer> countList = new ArrayList<>(counts.values());
        Collections.sort(countList, Collections.reverseOrder());
        
        HandRank rank = new HandRank();
        
        // 皇家同花顺
        if (isFlush && isStraight && values[4] == 12 && values[0] == 8) {
            rank.rank = ROYAL_FLUSH;
            rank.rankName = "皇家同花顺";
            rank.kickers = new int[]{values[4]};
        }
        // 同花顺
        else if (isFlush && isStraight) {
            rank.rank = STRAIGHT_FLUSH;
            rank.rankName = "同花顺";
            rank.kickers = new int[]{values[4]};
        }
        // 四条
        else if (countList.get(0) == 4) {
            rank.rank = FOUR_OF_KIND;
            rank.rankName = "四条";
            rank.kickers = getKickers(counts, 4);
        }
        // 葫芦
        else if (countList.get(0) == 3 && countList.get(1) == 2) {
            rank.rank = FULL_HOUSE;
            rank.rankName = "葫芦";
            rank.kickers = getKickers(counts, 3);
        }
        // 同花
        else if (isFlush) {
            rank.rank = FLUSH;
            rank.rankName = "同花";
            rank.kickers = reverse(values);
        }
        // 顺子
        else if (isStraight) {
            rank.rank = STRAIGHT;
            rank.rankName = "顺子";
            rank.kickers = new int[]{values[4]};
        }
        // 三条
        else if (countList.get(0) == 3) {
            rank.rank = THREE_OF_KIND;
            rank.rankName = "三条";
            rank.kickers = getKickers(counts, 3);
        }
        // 两对
        else if (countList.get(0) == 2 && countList.get(1) == 2) {
            rank.rank = TWO_PAIR;
            rank.rankName = "两对";
            rank.kickers = getKickers(counts, 2);
        }
        // 一对
        else if (countList.get(0) == 2) {
            rank.rank = ONE_PAIR;
            rank.rankName = "一对";
            rank.kickers = getKickers(counts, 2);
        }
        // 高牌
        else {
            rank.rank = HIGH_CARD;
            rank.rankName = "高牌";
            rank.kickers = reverse(values);
        }
        
        return rank;
    }
    
    private static boolean isStraight(int[] values) {
        // 普通顺子
        for (int i = 0; i < 4; i++) {
            if (values[i + 1] - values[i] != 1) {
                // 检查 A2345
                if (values[0] == 0 && values[1] == 1 && values[2] == 2 
                    && values[3] == 3 && values[4] == 12) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }
    
    private static int[] getKickers(Map<Integer, Integer> counts, int mainCount) {
        List<Integer> main = new ArrayList<>();
        List<Integer> other = new ArrayList<>();
        
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() == mainCount) {
                main.add(e.getKey());
            } else {
                other.add(e.getKey());
            }
        }
        
        Collections.sort(main, Collections.reverseOrder());
        Collections.sort(other, Collections.reverseOrder());
        
        main.addAll(other);
        return main.stream().mapToInt(i -> i).toArray();
    }
    
    private static int[] reverse(int[] arr) {
        int[] result = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[arr.length - 1 - i];
        }
        return result;
    }
}
