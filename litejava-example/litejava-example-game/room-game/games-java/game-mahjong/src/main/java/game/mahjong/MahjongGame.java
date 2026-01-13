package game.mahjong;

import game.mahjong.vo.*;

import java.util.*;

/**
 * 麻将游戏逻辑 (四川麻将/血战到底)
 * 
 * 规则：
 * - 4人，108张牌 (万筒条各36张，无字牌)
 * - 可碰、杠，不可吃
 * - 血战到底：一家胡牌后继续，直到剩一家
 */
public class MahjongGame {
    
    // 游戏状态
    public static final int STATUS_WAIT = 0;
    public static final int STATUS_PLAYING = 1;
    public static final int STATUS_OVER = 2;
    
    public int status = STATUS_WAIT;
    public int currentSeat;      // 当前操作玩家
    public int dealerSeat;       // 庄家
    
    // 牌堆
    private List<Integer> deck = new ArrayList<>();
    
    // 玩家手牌 [seatIndex][cards]
    public List<List<Integer>> hands = new ArrayList<>();
    
    // 玩家明牌 (碰/杠)
    public List<List<int[]>> melds = new ArrayList<>();
    
    // 玩家出的牌
    public List<List<Integer>> discards = new ArrayList<>();
    
    // 最后出的牌
    public int lastDiscard = -1;
    public int lastDiscardSeat = -1;
    
    // 胡牌玩家
    public List<Integer> winners = new ArrayList<>();
    public boolean[] finished;  // 是否已胡牌
    
    public MahjongGame(int playerCount) {
        for (int i = 0; i < playerCount; i++) {
            hands.add(new ArrayList<>());
            melds.add(new ArrayList<>());
            discards.add(new ArrayList<>());
        }
        finished = new boolean[playerCount];
        dealerSeat = new Random().nextInt(playerCount);
        currentSeat = dealerSeat;
    }
    
    /**
     * 洗牌发牌
     */
    public void deal() {
        // 生成牌 (万筒条各1-9，每种4张)
        deck.clear();
        for (int type = 0; type < 3; type++) {
            for (int num = 1; num <= 9; num++) {
                int card = type * 10 + num;  // 1-9万, 11-19筒, 21-29条
                for (int i = 0; i < 4; i++) {
                    deck.add(card);
                }
            }
        }
        Collections.shuffle(deck);
        
        // 发牌：每人13张，庄家14张
        for (int i = 0; i < hands.size(); i++) {
            hands.get(i).clear();
            int count = (i == dealerSeat) ? 14 : 13;
            for (int j = 0; j < count; j++) {
                hands.get(i).add(deck.remove(0));
            }
            Collections.sort(hands.get(i));
        }
        
        status = STATUS_PLAYING;
    }
    
    /**
     * 摸牌
     */
    public int draw(int seatIndex) {
        if (deck.isEmpty()) return -1;
        
        int card = deck.remove(0);
        hands.get(seatIndex).add(card);
        return card;
    }
    
    /**
     * 出牌
     */
    public MjDiscardResultVO discard(int seatIndex, int card) {
        if (seatIndex != currentSeat) return null;
        if (finished[seatIndex]) return null;
        
        List<Integer> hand = hands.get(seatIndex);
        if (!hand.remove(Integer.valueOf(card))) return null;
        
        discards.get(seatIndex).add(card);
        lastDiscard = card;
        lastDiscardSeat = seatIndex;
        
        MjDiscardResultVO result = new MjDiscardResultVO();
        result.seatIndex = seatIndex;
        result.card = card;
        result.actions = checkActions(card, seatIndex);
        
        // 如果没人能操作，轮到下家
        if (result.actions.isEmpty()) {
            nextTurn();
            result.nextSeat = currentSeat;
        }
        
        return result;
    }
    
    /**
     * 碰牌
     */
    public MjPengResultVO peng(int seatIndex) {
        if (lastDiscard < 0 || finished[seatIndex]) return null;
        if (seatIndex == lastDiscardSeat) return null;
        
        List<Integer> hand = hands.get(seatIndex);
        int count = 0;
        for (int c : hand) {
            if (c == lastDiscard) count++;
        }
        if (count < 2) return null;
        
        int pengCard = lastDiscard;
        
        // 移除手牌中的两张
        hand.remove(Integer.valueOf(pengCard));
        hand.remove(Integer.valueOf(pengCard));
        
        // 添加到明牌
        melds.get(seatIndex).add(new int[]{0, pengCard, pengCard, pengCard});  // 0=碰
        
        // 从出牌者的弃牌中移除
        discards.get(lastDiscardSeat).remove(Integer.valueOf(pengCard));
        
        currentSeat = seatIndex;
        lastDiscard = -1;
        
        MjPengResultVO result = new MjPengResultVO();
        result.seatIndex = seatIndex;
        result.card = pengCard;
        return result;
    }
    
    /**
     * 杠牌
     */
    public MjGangResultVO gang(int seatIndex, int card, boolean isSelf) {
        if (finished[seatIndex]) return null;
        
        List<Integer> hand = hands.get(seatIndex);
        String gangType;
        
        if (isSelf) {
            // 自摸杠 (暗杠或补杠)
            int count = 0;
            for (int c : hand) {
                if (c == card) count++;
            }
            
            if (count == 4) {
                // 暗杠
                for (int i = 0; i < 4; i++) {
                    hand.remove(Integer.valueOf(card));
                }
                melds.get(seatIndex).add(new int[]{2, card, card, card, card});  // 2=暗杠
                gangType = "angang";
            } else if (count == 1) {
                // 补杠 (已碰的牌)
                boolean found = false;
                for (int[] meld : melds.get(seatIndex)) {
                    if (meld[0] == 0 && meld[1] == card) {
                        meld[0] = 1;  // 改为明杠
                        found = true;
                        break;
                    }
                }
                if (!found) return null;
                hand.remove(Integer.valueOf(card));
                gangType = "bugang";
            } else {
                return null;
            }
        } else {
            // 点杠
            if (lastDiscard != card) return null;
            int count = 0;
            for (int c : hand) {
                if (c == card) count++;
            }
            if (count < 3) return null;
            
            for (int i = 0; i < 3; i++) {
                hand.remove(Integer.valueOf(card));
            }
            melds.get(seatIndex).add(new int[]{1, card, card, card, card});  // 1=明杠
            discards.get(lastDiscardSeat).remove(Integer.valueOf(lastDiscard));
            gangType = "minggang";
        }
        
        currentSeat = seatIndex;
        lastDiscard = -1;
        
        // 杠后摸牌
        int drawn = draw(seatIndex);
        
        MjGangResultVO result = new MjGangResultVO();
        result.seatIndex = seatIndex;
        result.card = card;
        result.type = gangType;
        result.drawn = drawn;
        return result;
    }
    
    /**
     * 胡牌
     */
    public MjHuResultVO hu(int seatIndex, boolean isSelfDrawn) {
        if (finished[seatIndex]) return null;
        
        List<Integer> hand = new ArrayList<>(hands.get(seatIndex));
        if (!isSelfDrawn && lastDiscard >= 0) {
            hand.add(lastDiscard);
        }
        
        if (!canHu(hand)) return null;
        
        finished[seatIndex] = true;
        winners.add(seatIndex);
        
        // 检查是否游戏结束 (只剩一家)
        int remaining = 0;
        for (int i = 0; i < finished.length; i++) {
            if (!finished[i]) remaining++;
        }
        
        MjHuResultVO result = new MjHuResultVO();
        result.seatIndex = seatIndex;
        result.selfDrawn = isSelfDrawn;
        result.hand = hand;
        
        if (remaining <= 1 || deck.isEmpty()) {
            status = STATUS_OVER;
            result.gameOver = true;
            result.winners = new ArrayList<>(winners);
        } else {
            // 继续游戏
            if (!isSelfDrawn) {
                discards.get(lastDiscardSeat).remove(Integer.valueOf(lastDiscard));
            }
            nextTurn();
            result.nextSeat = currentSeat;
        }
        
        lastDiscard = -1;
        return result;
    }
    
    /**
     * 过 (放弃操作)
     */
    public void pass(int seatIndex) {
        // 简化处理：直接轮到下家
        nextTurn();
    }
    
    private void nextTurn() {
        do {
            currentSeat = (currentSeat + 1) % hands.size();
        } while (finished[currentSeat] && countRemaining() > 1);
    }
    
    private int countRemaining() {
        int count = 0;
        for (boolean f : finished) {
            if (!f) count++;
        }
        return count;
    }
    
    private List<MjActionVO> checkActions(int card, int fromSeat) {
        List<MjActionVO> actions = new ArrayList<>();
        
        for (int i = 0; i < hands.size(); i++) {
            if (i == fromSeat || finished[i]) continue;
            
            List<Integer> hand = hands.get(i);
            int count = 0;
            for (int c : hand) {
                if (c == card) count++;
            }
            
            List<String> ops = new ArrayList<>();
            
            if (count >= 2) ops.add("peng");
            if (count >= 3) ops.add("gang");
            
            // 检查胡
            List<Integer> testHand = new ArrayList<>(hand);
            testHand.add(card);
            if (canHu(testHand)) ops.add("hu");
            
            if (!ops.isEmpty()) {
                MjActionVO action = new MjActionVO();
                action.seatIndex = i;
                action.actions = ops;
                actions.add(action);
            }
        }
        
        return actions;
    }
    
    /**
     * 简化胡牌判断 (3n+2)
     */
    private boolean canHu(List<Integer> hand) {
        if (hand.size() % 3 != 2) return false;
        
        int[] counts = new int[30];
        for (int c : hand) {
            counts[c]++;
        }
        
        // 尝试每种牌作为将
        for (int i = 1; i <= 29; i++) {
            if (counts[i] >= 2) {
                counts[i] -= 2;
                if (canMeld(counts)) {
                    counts[i] += 2;
                    return true;
                }
                counts[i] += 2;
            }
        }
        return false;
    }
    
    private boolean canMeld(int[] counts) {
        for (int i = 1; i <= 29; i++) {
            if (counts[i] > 0) {
                // 尝试刻子
                if (counts[i] >= 3) {
                    counts[i] -= 3;
                    if (canMeld(counts)) {
                        counts[i] += 3;
                        return true;
                    }
                    counts[i] += 3;
                }
                // 尝试顺子
                int num = i % 10;
                if (num <= 7 && counts[i] > 0 && counts[i + 1] > 0 && counts[i + 2] > 0) {
                    counts[i]--;
                    counts[i + 1]--;
                    counts[i + 2]--;
                    if (canMeld(counts)) {
                        counts[i]++;
                        counts[i + 1]++;
                        counts[i + 2]++;
                        return true;
                    }
                    counts[i]++;
                    counts[i + 1]++;
                    counts[i + 2]++;
                }
                return false;
            }
        }
        return true;
    }
    
    public List<Integer> getPlayerHand(int seatIndex) {
        return new ArrayList<>(hands.get(seatIndex));
    }
    
    public int getRemainingCards() {
        return deck.size();
    }
}
