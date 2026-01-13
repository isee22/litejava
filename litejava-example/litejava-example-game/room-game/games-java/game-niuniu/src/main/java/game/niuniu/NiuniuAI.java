package game.niuniu;

import java.util.*;

/**
 * 牛牛 AI
 */
public class NiuniuAI {
    
    /**
     * 选择下注倍数
     * @param cards 前4张牌
     * @return 倍数 (1-4)
     */
    public static int selectBetMultiple(int[] cards) {
        // 根据牌面估算牛型概率
        int potential = estimatePotential(cards);
        
        if (potential >= 80) return 4;
        if (potential >= 60) return 3;
        if (potential >= 40) return 2;
        return 1;
    }
    
    /**
     * 计算牛型
     * @param cards 5张牌
     * @return 牛型 (0=没牛, 1-9=牛一到牛九, 10=牛牛)
     */
    public static int calculateNiu(int[] cards) {
        if (cards.length != 5) return 0;
        
        int[] values = new int[5];
        for (int i = 0; i < 5; i++) {
            int v = cards[i] % 13 + 1;  // 1-13
            values[i] = Math.min(v, 10);  // J/Q/K 算10
        }
        
        // 枚举3张牌组合，看是否能凑成10的倍数
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 4; j++) {
                for (int k = j + 1; k < 5; k++) {
                    int sum3 = values[i] + values[j] + values[k];
                    if (sum3 % 10 == 0) {
                        // 剩余两张
                        int sum2 = 0;
                        for (int m = 0; m < 5; m++) {
                            if (m != i && m != j && m != k) {
                                sum2 += values[m];
                            }
                        }
                        int niu = sum2 % 10;
                        return niu == 0 ? 10 : niu;
                    }
                }
            }
        }
        
        return 0;  // 没牛
    }
    
    /**
     * 比较两手牌大小
     * @return >0 表示 cards1 大, <0 表示 cards2 大, 0 表示相等
     */
    public static int compare(int[] cards1, int[] cards2) {
        int niu1 = calculateNiu(cards1);
        int niu2 = calculateNiu(cards2);
        
        if (niu1 != niu2) {
            return niu1 - niu2;
        }
        
        // 牛型相同，比最大牌
        int max1 = getMaxCard(cards1);
        int max2 = getMaxCard(cards2);
        return max1 - max2;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 估算潜力 (0-100)
     */
    private static int estimatePotential(int[] cards) {
        if (cards.length < 4) return 50;
        
        int[] values = new int[4];
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            int v = cards[i] % 13 + 1;
            values[i] = Math.min(v, 10);
            sum += values[i];
        }
        
        // 检查3张能否凑10
        for (int i = 0; i < 2; i++) {
            for (int j = i + 1; j < 3; j++) {
                for (int k = j + 1; k < 4; k++) {
                    int sum3 = values[i] + values[j] + values[k];
                    if (sum3 % 10 == 0) {
                        return 90;  // 已经有3张凑10
                    }
                    // 差多少能凑10
                    int need = (10 - sum3 % 10) % 10;
                    if (need <= 3) return 70;  // 容易凑
                }
            }
        }
        
        // 看总和
        int mod = sum % 10;
        if (mod <= 3 || mod >= 7) return 60;
        
        return 40;
    }
    
    /**
     * 获取最大牌
     */
    private static int getMaxCard(int[] cards) {
        int max = 0;
        for (int card : cards) {
            int v = card % 13;
            if (v == 0) v = 13;  // A 最大
            if (v > max) max = v;
        }
        return max;
    }
}
