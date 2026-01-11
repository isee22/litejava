package game.niuniu;

import java.util.*;

/**
 * 牛牛游戏逻辑
 * 
 * 规则：
 * - 2-6人，每人5张牌
 * - 3张牌凑成10的倍数为"牛"，剩余2张点数之和取个位为牛几
 * - 牛牛 > 牛9 > ... > 牛1 > 无牛
 */
public class NiuniuGame {
    
    // 游戏阶段
    public static final int PHASE_BET = 0;    // 下注
    public static final int PHASE_SHOW = 1;   // 亮牌
    public static final int PHASE_OVER = 2;
    
    public int phase = PHASE_BET;
    public int bankerSeat;  // 庄家
    
    // 牌
    private List<Integer> deck = new ArrayList<>();
    public int[][] cards;   // [seatIndex][5]
    
    // 下注
    public int[] bets;      // 下注倍数
    public boolean[] shown; // 是否已亮牌
    
    // 结果
    public int[] results;   // 牛几 (0=无牛, 10=牛牛, 11=五花牛, 12=炸弹, 13=五小牛)
    public int[] scores;    // 得分
    
    public boolean isOver;
    
    public NiuniuGame(int playerCount) {
        cards = new int[playerCount][5];
        bets = new int[playerCount];
        shown = new boolean[playerCount];
        results = new int[playerCount];
        scores = new int[playerCount];
        
        bankerSeat = new Random().nextInt(playerCount);
        bets[bankerSeat] = 1;  // 庄家默认下注1
    }
    
    /**
     * 发牌
     */
    public void deal() {
        deck.clear();
        for (int i = 0; i < 52; i++) {
            deck.add(i);
        }
        Collections.shuffle(deck);
        
        for (int i = 0; i < cards.length; i++) {
            for (int j = 0; j < 5; j++) {
                cards[i][j] = deck.remove(0);
            }
        }
    }
    
    /**
     * 下注 (闲家)
     */
    public Map<String, Object> bet(int seatIndex, int multiple) {
        if (seatIndex == bankerSeat) return null;  // 庄家不能下注
        if (bets[seatIndex] > 0) return null;      // 已下注
        if (multiple < 1 || multiple > 4) return null;
        
        bets[seatIndex] = multiple;
        
        Map<String, Object> result = new HashMap<>();
        result.put("seatIndex", seatIndex);
        result.put("multiple", multiple);
        return result;
    }
    
    /**
     * 是否所有人都下注了
     */
    public boolean allBet() {
        for (int i = 0; i < bets.length; i++) {
            if (bets[i] == 0) return false;
        }
        phase = PHASE_SHOW;
        return true;
    }
    
    /**
     * 亮牌
     */
    public Map<String, Object> show(int seatIndex) {
        if (phase != PHASE_SHOW) return null;
        if (shown[seatIndex]) return null;
        
        shown[seatIndex] = true;
        results[seatIndex] = calculateNiu(cards[seatIndex]);
        
        Map<String, Object> result = new HashMap<>();
        result.put("seatIndex", seatIndex);
        result.put("cards", cards[seatIndex].clone());
        result.put("niu", results[seatIndex]);
        result.put("niuName", getNiuName(results[seatIndex]));
        return result;
    }
    
    /**
     * 是否所有人都亮牌了
     */
    public boolean allShown() {
        for (boolean s : shown) {
            if (!s) return false;
        }
        return true;
    }
    
    /**
     * 结算
     */
    public Map<String, Object> settle() {
        phase = PHASE_OVER;
        isOver = true;
        
        int bankerNiu = results[bankerSeat];
        
        for (int i = 0; i < cards.length; i++) {
            if (i == bankerSeat) continue;
            
            int playerNiu = results[i];
            int multiple = bets[i];
            int niuMultiple = getNiuMultiple(playerNiu);
            
            if (compareNiu(playerNiu, bankerNiu) > 0) {
                // 闲家赢
                int win = multiple * niuMultiple;
                scores[i] = win;
                scores[bankerSeat] -= win;
            } else {
                // 庄家赢
                int win = multiple * getNiuMultiple(bankerNiu);
                scores[i] = -win;
                scores[bankerSeat] += win;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("bankerNiu", bankerNiu);
        result.put("results", results.clone());
        result.put("scores", scores.clone());
        return result;
    }
    
    /**
     * 计算牛几
     */
    private int calculateNiu(int[] hand) {
        int[] values = new int[5];
        int jqkCount = 0;
        int sum = 0;
        
        for (int i = 0; i < 5; i++) {
            int rank = hand[i] % 13;
            values[i] = Math.min(rank + 1, 10);  // J/Q/K = 10
            if (rank >= 10) jqkCount++;
            sum += values[i];
        }
        
        // 五小牛 (5张牌点数之和<=10)
        if (sum <= 10) return 13;
        
        // 炸弹 (4张相同)
        int[] rankCount = new int[13];
        for (int c : hand) {
            rankCount[c % 13]++;
        }
        for (int c : rankCount) {
            if (c == 4) return 12;
        }
        
        // 五花牛 (5张都是JQK)
        if (jqkCount == 5) return 11;
        
        // 普通牛
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                for (int k = j + 1; k < 5; k++) {
                    if ((values[i] + values[j] + values[k]) % 10 == 0) {
                        // 找到牛，计算剩余两张
                        int remain = 0;
                        for (int m = 0; m < 5; m++) {
                            if (m != i && m != j && m != k) {
                                remain += values[m];
                            }
                        }
                        int niu = remain % 10;
                        return niu == 0 ? 10 : niu;  // 牛牛=10
                    }
                }
            }
        }
        
        return 0;  // 无牛
    }
    
    /**
     * 比较牛大小
     */
    private int compareNiu(int a, int b) {
        return Integer.compare(a, b);
    }
    
    /**
     * 获取牛的倍数
     */
    private int getNiuMultiple(int niu) {
        if (niu >= 11) return 5;  // 五小牛/炸弹/五花牛
        if (niu == 10) return 3;  // 牛牛
        if (niu >= 7) return 2;   // 牛7-牛9
        return 1;                  // 牛1-牛6/无牛
    }
    
    /**
     * 获取牛名称
     */
    private String getNiuName(int niu) {
        if (niu == 13) return "五小牛";
        if (niu == 12) return "炸弹";
        if (niu == 11) return "五花牛";
        if (niu == 10) return "牛牛";
        if (niu == 0) return "无牛";
        return "牛" + niu;
    }
    
    public int[] getCards(int seatIndex) {
        return cards[seatIndex].clone();
    }
    
    public int[] getFirstFourCards(int seatIndex) {
        return Arrays.copyOf(cards[seatIndex], 4);
    }
    
    public int getFifthCard(int seatIndex) {
        return cards[seatIndex][4];
    }
}
