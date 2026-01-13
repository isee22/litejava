package game.texas;

import java.util.*;

/**
 * 德州扑克 AI
 * 
 * 策略：根据手牌强度和底池赔率决策
 */
public class TexasAI {
    
    /**
     * 决策动作
     */
    public static class Decision {
        public String action;  // fold, check, call, raise, allin
        public int amount;     // raise 金额
    }
    
    /**
     * 做出决策
     * @param holeCards 手牌 (2张)
     * @param communityCards 公共牌
     * @param pot 底池
     * @param currentBet 当前下注
     * @param myBet 我的下注
     * @param myChips 我的筹码
     * @param stage 阶段 (0=preflop, 1=flop, 2=turn, 3=river)
     */
    public static Decision decide(int[] holeCards, int[] communityCards,
                                  int pot, int currentBet, int myBet, int myChips, int stage) {
        Decision d = new Decision();
        
        int toCall = currentBet - myBet;
        int handStrength = evaluateHand(holeCards, communityCards, stage);
        
        // 计算底池赔率
        double potOdds = toCall > 0 ? (double) toCall / (pot + toCall) : 0;
        
        // 根据手牌强度决策
        if (handStrength >= 90) {
            // 超强牌：加注或全下
            d.action = "raise";
            d.amount = Math.min(pot, myChips);
            if (d.amount > myChips * 0.5) {
                d.action = "allin";
            }
        } else if (handStrength >= 70) {
            // 强牌：加注
            d.action = "raise";
            d.amount = pot / 2;
        } else if (handStrength >= 50) {
            // 中等牌：跟注
            if (toCall == 0) {
                d.action = "check";
            } else if (toCall <= myChips * 0.2) {
                d.action = "call";
            } else {
                d.action = "fold";
            }
        } else if (handStrength >= 30) {
            // 弱牌：小注跟，大注弃
            if (toCall == 0) {
                d.action = "check";
            } else if (toCall <= myChips * 0.1) {
                d.action = "call";
            } else {
                d.action = "fold";
            }
        } else {
            // 垃圾牌
            if (toCall == 0) {
                d.action = "check";
            } else {
                d.action = "fold";
            }
        }
        
        return d;
    }
    
    /**
     * 评估手牌强度 (0-100)
     */
    private static int evaluateHand(int[] holeCards, int[] communityCards, int stage) {
        if (stage == 0) {
            // 翻牌前：只看手牌
            return evaluatePreflop(holeCards);
        }
        
        // 翻牌后：计算最佳5张牌
        int[] allCards = new int[holeCards.length + communityCards.length];
        System.arraycopy(holeCards, 0, allCards, 0, holeCards.length);
        System.arraycopy(communityCards, 0, allCards, holeCards.length, communityCards.length);
        
        return evaluatePostflop(allCards);
    }
    
    /**
     * 翻牌前手牌评估
     */
    private static int evaluatePreflop(int[] cards) {
        if (cards.length < 2) return 0;
        
        int v1 = cards[0] % 13;  // 0-12 (2-A)
        int v2 = cards[1] % 13;
        int s1 = cards[0] / 13;  // 花色
        int s2 = cards[1] / 13;
        
        boolean suited = (s1 == s2);
        boolean pair = (v1 == v2);
        int high = Math.max(v1, v2);
        int low = Math.min(v1, v2);
        int gap = high - low;
        
        int score = 0;
        
        // 对子
        if (pair) {
            score = 50 + high * 3;
            if (high >= 10) score += 20;  // 大对子
        } else {
            // 高牌
            score = high * 2 + low;
            
            // 同花
            if (suited) score += 10;
            
            // 连张
            if (gap == 1) score += 10;
            else if (gap == 2) score += 5;
            
            // AK, AQ 等
            if (high == 12) {  // A
                if (low >= 10) score += 15;
            }
        }
        
        return Math.min(100, score);
    }
    
    /**
     * 翻牌后评估
     */
    private static int evaluatePostflop(int[] cards) {
        // 简化：检查基本牌型
        Map<Integer, Integer> valueCounts = new HashMap<>();
        Map<Integer, Integer> suitCounts = new HashMap<>();
        
        for (int card : cards) {
            int value = card % 13;
            int suit = card / 13;
            valueCounts.merge(value, 1, Integer::sum);
            suitCounts.merge(suit, 1, Integer::sum);
        }
        
        // 检查同花
        boolean hasFlush = suitCounts.values().stream().anyMatch(c -> c >= 5);
        
        // 检查对子、三条、四条
        int pairs = 0, trips = 0, quads = 0;
        for (int count : valueCounts.values()) {
            if (count == 2) pairs++;
            else if (count == 3) trips++;
            else if (count == 4) quads++;
        }
        
        // 评分
        if (quads > 0) return 95;
        if (trips > 0 && pairs > 0) return 90;  // 葫芦
        if (hasFlush) return 85;
        if (trips > 0) return 70;
        if (pairs >= 2) return 55;
        if (pairs == 1) return 40;
        
        // 高牌
        int maxValue = valueCounts.keySet().stream().max(Integer::compare).orElse(0);
        return 20 + maxValue;
    }
}
