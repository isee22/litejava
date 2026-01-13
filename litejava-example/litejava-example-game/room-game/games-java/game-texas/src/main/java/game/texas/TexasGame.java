package game.texas;

import java.util.*;

/**
 * 德州扑克游戏逻辑
 * 
 * 规则：
 * - 2-9人，每人2张底牌，5张公共牌
 * - 四轮下注：翻牌前、翻牌、转牌、河牌
 * - 最大5张组合比大小
 */
public class TexasGame {
    
    // 游戏阶段
    public static final int STAGE_PREFLOP = 0;   // 翻牌前
    public static final int STAGE_FLOP = 1;      // 翻牌 (3张公共牌)
    public static final int STAGE_TURN = 2;      // 转牌 (第4张)
    public static final int STAGE_RIVER = 3;     // 河牌 (第5张)
    public static final int STAGE_SHOWDOWN = 4;  // 摊牌
    public static final int STAGE_OVER = 5;
    
    public int stage = STAGE_PREFLOP;
    public int currentSeat;
    public int dealerSeat;      // 庄家
    public int smallBlindSeat;  // 小盲
    public int bigBlindSeat;    // 大盲
    
    // 牌
    private List<Integer> deck = new ArrayList<>();
    public int[][] holeCards;   // 底牌 [seatIndex][2]
    public int[] communityCards = new int[5];  // 公共牌
    public int communityCount = 0;
    
    // 筹码
    public int[] chips;         // 玩家筹码
    public int[] bets;          // 当前轮下注
    public int[] totalBets;     // 总下注
    public int pot;             // 底池
    public int currentBet;      // 当前最高下注
    
    // 状态
    public boolean[] folded;    // 是否弃牌
    public boolean[] allIn;     // 是否全下
    public int smallBlind = 10;
    public int bigBlind = 20;
    
    public boolean isOver;
    public int winner = -1;
    public int[] winnerHand;
    
    public TexasGame(int playerCount, int startChips) {
        holeCards = new int[playerCount][2];
        chips = new int[playerCount];
        bets = new int[playerCount];
        totalBets = new int[playerCount];
        folded = new boolean[playerCount];
        allIn = new boolean[playerCount];
        
        Arrays.fill(chips, startChips);
        
        dealerSeat = new Random().nextInt(playerCount);
        smallBlindSeat = (dealerSeat + 1) % playerCount;
        bigBlindSeat = (dealerSeat + 2) % playerCount;
        currentSeat = (bigBlindSeat + 1) % playerCount;
    }
    
    /**
     * 发牌
     */
    public void deal() {
        // 生成牌 (0-51: 花色*13+点数)
        deck.clear();
        for (int i = 0; i < 52; i++) {
            deck.add(i);
        }
        Collections.shuffle(deck);
        
        // 发底牌
        for (int i = 0; i < holeCards.length; i++) {
            holeCards[i][0] = deck.remove(0);
            holeCards[i][1] = deck.remove(0);
        }
        
        // 收盲注
        bet(smallBlindSeat, smallBlind);
        bet(bigBlindSeat, bigBlind);
        currentBet = bigBlind;
    }
    
    /**
     * 下注
     */
    public Map<String, Object> bet(int seatIndex, int amount) {
        if (folded[seatIndex] || allIn[seatIndex]) return null;
        
        int actual = Math.min(amount, chips[seatIndex]);
        chips[seatIndex] -= actual;
        bets[seatIndex] += actual;
        totalBets[seatIndex] += actual;
        pot += actual;
        
        if (chips[seatIndex] == 0) {
            allIn[seatIndex] = true;
        }
        
        if (bets[seatIndex] > currentBet) {
            currentBet = bets[seatIndex];
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("seatIndex", seatIndex);
        result.put("amount", actual);
        result.put("chips", chips[seatIndex]);
        result.put("pot", pot);
        result.put("currentBet", currentBet);
        return result;
    }
    
    /**
     * 跟注
     */
    public Map<String, Object> call(int seatIndex) {
        if (seatIndex != currentSeat) return null;
        
        int toCall = currentBet - bets[seatIndex];
        Map<String, Object> result = bet(seatIndex, toCall);
        if (result != null) {
            result.put("action", "call");
            nextPlayer();
            result.put("nextSeat", currentSeat);
        }
        return result;
    }
    
    /**
     * 加注
     */
    public Map<String, Object> raise(int seatIndex, int amount) {
        if (seatIndex != currentSeat) return null;
        
        int toCall = currentBet - bets[seatIndex];
        int total = toCall + amount;
        
        Map<String, Object> result = bet(seatIndex, total);
        if (result != null) {
            result.put("action", "raise");
            result.put("raiseAmount", amount);
            nextPlayer();
            result.put("nextSeat", currentSeat);
        }
        return result;
    }
    
    /**
     * 过牌
     */
    public Map<String, Object> check(int seatIndex) {
        if (seatIndex != currentSeat) return null;
        if (bets[seatIndex] < currentBet) return null;  // 不能过牌
        
        Map<String, Object> result = new HashMap<>();
        result.put("seatIndex", seatIndex);
        result.put("action", "check");
        
        nextPlayer();
        result.put("nextSeat", currentSeat);
        return result;
    }
    
    /**
     * 弃牌
     */
    public Map<String, Object> fold(int seatIndex) {
        if (seatIndex != currentSeat) return null;
        
        folded[seatIndex] = true;
        
        Map<String, Object> result = new HashMap<>();
        result.put("seatIndex", seatIndex);
        result.put("action", "fold");
        
        // 检查是否只剩一人
        int remaining = countActive();
        if (remaining == 1) {
            for (int i = 0; i < folded.length; i++) {
                if (!folded[i]) {
                    winner = i;
                    break;
                }
            }
            isOver = true;
            stage = STAGE_OVER;
            chips[winner] += pot;
            result.put("gameOver", true);
            result.put("winner", winner);
            result.put("pot", pot);
        } else {
            nextPlayer();
            result.put("nextSeat", currentSeat);
        }
        
        return result;
    }
    
    /**
     * 全下
     */
    public Map<String, Object> allIn(int seatIndex) {
        if (seatIndex != currentSeat) return null;
        
        Map<String, Object> result = bet(seatIndex, chips[seatIndex]);
        if (result != null) {
            result.put("action", "allin");
            nextPlayer();
            result.put("nextSeat", currentSeat);
        }
        return result;
    }
    
    private void nextPlayer() {
        int start = currentSeat;
        do {
            currentSeat = (currentSeat + 1) % chips.length;
            
            // 检查是否完成一轮
            if (isRoundComplete()) {
                nextStage();
                return;
            }
        } while ((folded[currentSeat] || allIn[currentSeat]) && currentSeat != start);
    }
    
    private boolean isRoundComplete() {
        for (int i = 0; i < chips.length; i++) {
            if (!folded[i] && !allIn[i] && bets[i] < currentBet) {
                return false;
            }
        }
        
        // 检查是否所有人都行动过
        int active = countActive();
        int allInCount = 0;
        for (boolean a : allIn) if (a) allInCount++;
        
        return active - allInCount <= 1 || currentSeat == (bigBlindSeat + 1) % chips.length;
    }
    
    private void nextStage() {
        // 重置下注
        Arrays.fill(bets, 0);
        currentBet = 0;
        currentSeat = smallBlindSeat;
        
        // 跳过弃牌和全下的玩家
        while (folded[currentSeat] || allIn[currentSeat]) {
            currentSeat = (currentSeat + 1) % chips.length;
        }
        
        switch (stage) {
            case STAGE_PREFLOP:
                // 发3张公共牌
                communityCards[0] = deck.remove(0);
                communityCards[1] = deck.remove(0);
                communityCards[2] = deck.remove(0);
                communityCount = 3;
                stage = STAGE_FLOP;
                break;
                
            case STAGE_FLOP:
                communityCards[3] = deck.remove(0);
                communityCount = 4;
                stage = STAGE_TURN;
                break;
                
            case STAGE_TURN:
                communityCards[4] = deck.remove(0);
                communityCount = 5;
                stage = STAGE_RIVER;
                break;
                
            case STAGE_RIVER:
                showdown();
                break;
        }
    }
    
    private void showdown() {
        stage = STAGE_SHOWDOWN;
        
        int bestRank = -1;
        for (int i = 0; i < chips.length; i++) {
            if (folded[i]) continue;
            
            int rank = evaluateHand(i);
            if (rank > bestRank) {
                bestRank = rank;
                winner = i;
            }
        }
        
        chips[winner] += pot;
        isOver = true;
        stage = STAGE_OVER;
    }
    
    /**
     * 简化牌型评估 (返回分数，越高越好)
     */
    private int evaluateHand(int seatIndex) {
        // 合并底牌和公共牌
        int[] cards = new int[7];
        cards[0] = holeCards[seatIndex][0];
        cards[1] = holeCards[seatIndex][1];
        for (int i = 0; i < communityCount; i++) {
            cards[2 + i] = communityCards[i];
        }
        
        // 简化：只比较最大牌点数
        int maxRank = 0;
        for (int c : cards) {
            if (c >= 0) {
                int rank = c % 13;
                if (rank == 0) rank = 13;  // A最大
                if (rank > maxRank) maxRank = rank;
            }
        }
        return maxRank;
    }
    
    private int countActive() {
        int count = 0;
        for (boolean f : folded) {
            if (!f) count++;
        }
        return count;
    }
    
    public int[] getHoleCards(int seatIndex) {
        return holeCards[seatIndex].clone();
    }
    
    public int[] getCommunityCards() {
        return Arrays.copyOf(communityCards, communityCount);
    }
    
    /**
     * 牌面转字符串 (调试用)
     */
    public static String cardToString(int card) {
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        return suits[card / 13] + ranks[card % 13];
    }
}
