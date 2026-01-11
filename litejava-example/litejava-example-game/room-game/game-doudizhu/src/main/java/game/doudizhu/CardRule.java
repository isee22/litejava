package game.doudizhu;

/**
 * 斗地主牌型规则
 * 
 * 牌的表示：0-53
 * 0-12: 方块 3-2
 * 13-25: 梅花 3-2
 * 26-38: 红桃 3-2
 * 39-51: 黑桃 3-2
 * 52: 小王
 * 53: 大王
 */
public class CardRule {
    
    /**
     * 获取牌型
     */
    public static CardType getType(int[] cards) {
        if (cards == null || cards.length == 0) return null;
        
        int[] valueCount = getValueCount(cards);
        int size = cards.length;
        
        // 单张
        if (size == 1) return CardType.SINGLE;
        
        // 对子
        if (size == 2 && isSameValue(cards)) return CardType.PAIR;
        
        // 王炸
        if (size == 2 && isJokerBomb(cards)) return CardType.JOKER_BOMB;
        
        // 三张
        if (size == 3 && isSameValue(cards)) return CardType.THREE;
        
        // 炸弹
        if (size == 4 && isSameValue(cards)) return CardType.BOMB;
        
        // 三带一
        if (size == 4 && hasNOfKind(valueCount, 3) == 1 && hasNOfKind(valueCount, 1) == 1) {
            return CardType.THREE_ONE;
        }
        
        // 三带二
        if (size == 5 && hasNOfKind(valueCount, 3) == 1 && hasNOfKind(valueCount, 2) == 1) {
            return CardType.THREE_TWO;
        }
        
        // 顺子 (5张及以上)
        if (size >= 5 && isSequence(valueCount, 1, size)) return CardType.STRAIGHT;
        
        // 连对 (3对及以上)
        if (size >= 6 && size % 2 == 0 && isSequence(valueCount, 2, size)) return CardType.STRAIGHT_PAIR;
        
        // 飞机不带
        if (size >= 6 && size % 3 == 0 && isPlane(valueCount, 0, size)) return CardType.PLANE;
        
        // 飞机带单
        if (size >= 8 && size % 4 == 0 && isPlane(valueCount, 1, size)) return CardType.PLANE_SINGLE;
        
        // 飞机带对
        if (size >= 10 && size % 5 == 0 && isPlane(valueCount, 2, size)) return CardType.PLANE_PAIR;
        
        // 四带二单
        if (size == 6 && hasNOfKind(valueCount, 4) == 1 && hasNOfKind(valueCount, 1) == 2) {
            return CardType.FOUR_TWO;
        }
        
        // 四带二对
        if (size == 8 && hasNOfKind(valueCount, 4) == 1 && hasNOfKind(valueCount, 2) == 2) {
            return CardType.FOUR_TWO_PAIR;
        }
        
        return null;
    }
    
    /**
     * 判断是否能压过
     */
    public static boolean canBeat(int[] last, int[] current) {
        CardType lastType = getType(last);
        CardType currentType = getType(current);
        
        if (lastType == null || currentType == null) return false;
        
        // 王炸最大
        if (currentType == CardType.JOKER_BOMB) return true;
        if (lastType == CardType.JOKER_BOMB) return false;
        
        // 炸弹能压非炸弹
        if (currentType == CardType.BOMB && lastType != CardType.BOMB) return true;
        if (lastType == CardType.BOMB && currentType != CardType.BOMB) return false;
        
        // 同类型比较
        if (lastType != currentType) return false;
        if (last.length != current.length) return false;
        
        int lastMax = getMainValue(last, lastType);
        int currentMax = getMainValue(current, currentType);
        
        return currentMax > lastMax;
    }
    
    /**
     * 获取牌的比较值
     * 3=0, 4=1, ..., K=10, A=11, 2=12, 小王=13, 大王=14
     */
    public static int getCardValue(int card) {
        if (card == 52) return 13; // 小王
        if (card == 53) return 14; // 大王
        return card % 13;
    }
    
    /**
     * 获取主牌点数
     */
    private static int getMainValue(int[] cards, CardType type) {
        int[] valueCount = getValueCount(cards);
        
        switch (type) {
            case SINGLE:
            case PAIR:
            case THREE:
            case BOMB:
                return getCardValue(cards[0]);
            case THREE_ONE:
            case THREE_TWO:
                for (int i = 14; i >= 0; i--) {
                    if (valueCount[i] == 3) return i;
                }
                break;
            case FOUR_TWO:
            case FOUR_TWO_PAIR:
                for (int i = 14; i >= 0; i--) {
                    if (valueCount[i] == 4) return i;
                }
                break;
            case STRAIGHT:
            case STRAIGHT_PAIR:
            case PLANE:
            case PLANE_SINGLE:
            case PLANE_PAIR:
                // 返回最小的牌（顺子比较起始点）
                for (int i = 0; i <= 11; i++) { // 不包含2和王
                    if (valueCount[i] > 0) return i;
                }
                break;
            default:
                break;
        }
        return 0;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 统计每个点数的数量
     */
    private static int[] getValueCount(int[] cards) {
        int[] count = new int[15]; // 0-12: 3到2, 13: 小王, 14: 大王
        for (int card : cards) {
            count[getCardValue(card)]++;
        }
        return count;
    }
    
    private static boolean isSameValue(int[] cards) {
        int value = getCardValue(cards[0]);
        for (int card : cards) {
            if (getCardValue(card) != value) return false;
        }
        return true;
    }
    
    private static boolean isJokerBomb(int[] cards) {
        return cards.length == 2 && 
               ((cards[0] == 52 && cards[1] == 53) || (cards[0] == 53 && cards[1] == 52));
    }
    
    private static int hasNOfKind(int[] valueCount, int n) {
        int count = 0;
        for (int i = 0; i <= 14; i++) {
            if (valueCount[i] == n) count++;
        }
        return count;
    }
    
    private static boolean isSequence(int[] valueCount, int countPerRank, int totalSize) {
        // 不能包含2和王 (value 12, 13, 14)
        if (valueCount[12] > 0 || valueCount[13] > 0 || valueCount[14] > 0) return false;
        
        int start = -1, end = -1;
        for (int i = 0; i <= 11; i++) { // 3到A
            if (valueCount[i] == countPerRank) {
                if (start < 0) start = i;
                end = i;
            } else if (valueCount[i] > 0) {
                return false;
            }
        }
        
        if (start < 0) return false;
        
        int length = end - start + 1;
        int expectedSize = length * countPerRank;
        
        return totalSize == expectedSize && length >= (countPerRank == 1 ? 5 : 3);
    }
    
    private static boolean isPlane(int[] valueCount, int wingType, int totalSize) {
        // 找连续的三张
        int planeStart = -1, planeEnd = -1;
        for (int i = 0; i <= 11; i++) { // 不包含2
            if (valueCount[i] >= 3) {
                if (planeStart < 0) planeStart = i;
                planeEnd = i;
            } else if (planeStart >= 0 && valueCount[i] < 3) {
                // 断了
                break;
            }
        }
        
        if (planeStart < 0 || planeEnd - planeStart < 1) return false;
        
        int planeCount = planeEnd - planeStart + 1;
        
        if (wingType == 0) {
            return totalSize == planeCount * 3;
        } else if (wingType == 1) {
            return totalSize == planeCount * 4;
        } else if (wingType == 2) {
            return totalSize == planeCount * 5;
        }
        
        return false;
    }
}

/**
 * 牌型枚举
 */
enum CardType {
    SINGLE,         // 单张
    PAIR,           // 对子
    THREE,          // 三张
    THREE_ONE,      // 三带一
    THREE_TWO,      // 三带二
    STRAIGHT,       // 顺子
    STRAIGHT_PAIR,  // 连对
    PLANE,          // 飞机不带
    PLANE_SINGLE,   // 飞机带单
    PLANE_PAIR,     // 飞机带对
    BOMB,           // 炸弹
    FOUR_TWO,       // 四带二单
    FOUR_TWO_PAIR,  // 四带二对
    JOKER_BOMB      // 王炸
}
