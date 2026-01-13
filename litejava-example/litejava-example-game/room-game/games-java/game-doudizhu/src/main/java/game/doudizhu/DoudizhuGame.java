package game.doudizhu;

import game.doudizhu.vo.*;

import java.util.*;

/**
 * 斗地主游戏逻辑
 * 
 * 纯游戏逻辑，不包含网络代码，方便单元测试
 * 
 * 游戏流程:
 * 1. deal() - 发牌
 * 2. bid() - 叫地主 (status=0)
 * 3. play()/pass() - 出牌 (status=1)
 * 4. 某玩家出完牌，游戏结束
 * 
 * 牌的编码:
 * - 0-12:  方块 3,4,5,6,7,8,9,10,J,Q,K,A,2
 * - 13-25: 梅花 3,4,5,6,7,8,9,10,J,Q,K,A,2
 * - 26-38: 红桃 3,4,5,6,7,8,9,10,J,Q,K,A,2
 * - 39-51: 黑桃 3,4,5,6,7,8,9,10,J,Q,K,A,2
 * - 52: 小王
 * - 53: 大王
 */
public class DoudizhuGame {
    
    // ==================== 配置 ====================
    
    /** 玩家数量 (3人或4人) */
    public final int playerCount;
    
    /** 底牌数量 */
    public final int bottomCount;
    
    /** 每人初始手牌数 */
    public final int handCount;
    
    // ==================== 玩家手牌 ====================
    
    /** 玩家手牌 [seatIndex][cardIndex] */
    public int[][] playerCards;
    
    /** 每个玩家当前手牌数量 */
    public int[] playerCardCount;
    
    /** 底牌 */
    public int[] bottomCards;
    
    // ==================== 游戏状态 ====================
    
    /** 游戏状态: 0=叫地主, 1=出牌 */
    public int status = 0;
    
    /** 当前操作的座位 */
    public int currentSeat = 0;
    
    /** 地主座位 (-1表示未确定) */
    public int landlordSeat = -1;
    
    // ==================== 叫地主阶段 ====================
    
    /** 已叫地主的人数 */
    public int bidCount = 0;
    
    /** 最后一个叫地主的座位 (-1表示无人叫) */
    public int lastBidSeat = -1;
    
    // ==================== 出牌阶段 ====================
    
    /** 上一手牌 (空数组表示新一轮) */
    public int[] lastCards = new int[0];
    
    /** 上一手牌的出牌者 */
    public int lastPlaySeat = -1;
    
    /** 连续不出的人数 */
    public int passCount = 0;
    
    /** 倍数 (炸弹翻倍) */
    public int multiple = 1;
    
    // ==================== 构造函数 ====================
    
    public DoudizhuGame() {
        this(3);
    }
    
    public DoudizhuGame(int playerCount) {
        this.playerCount = playerCount;
        
        if (playerCount == 3) {
            this.handCount = 17;
            this.bottomCount = 3;
        } else if (playerCount == 4) {
            this.handCount = 25;
            this.bottomCount = 8;
        } else {
            this.handCount = 17;
            this.bottomCount = 3;
        }
        
        this.playerCards = new int[playerCount][handCount + bottomCount];
        this.playerCardCount = new int[playerCount];
        this.bottomCards = new int[bottomCount];
    }
    
    // ==================== 发牌 ====================
    
    public void deal() {
        int[] deck;
        if (playerCount <= 3) {
            deck = createDeck(1);
        } else {
            deck = createDeck(2);
        }
        
        shuffle(deck);
        
        int idx = 0;
        for (int i = 0; i < playerCount; i++) {
            for (int j = 0; j < handCount; j++) {
                playerCards[i][j] = deck[idx++];
            }
            playerCardCount[i] = handCount;
            sortCards(playerCards[i], handCount);
        }
        
        for (int i = 0; i < bottomCount; i++) {
            bottomCards[i] = deck[idx++];
        }
        
        currentSeat = new Random().nextInt(playerCount);
        status = 0;
        bidCount = 0;
        lastBidSeat = -1;
    }
    
    public int[] getPlayerCards(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= playerCount) {
            return new int[0];
        }
        int count = playerCardCount[seatIndex];
        int[] result = new int[count];
        System.arraycopy(playerCards[seatIndex], 0, result, 0, count);
        return result;
    }
    
    // ==================== 叫地主 ====================
    
    /**
     * 叫地主
     */
    public DdzBidVO bid(int seatIndex, boolean wantBid) {
        if (status != 0) return null;
        if (seatIndex != currentSeat) return null;
        
        DdzBidVO result = new DdzBidVO();
        result.seat = currentSeat;
        result.bid = wantBid;
        
        if (wantBid) {
            lastBidSeat = currentSeat;
        }
        
        bidCount++;
        
        if (bidCount >= playerCount) {
            if (lastBidSeat >= 0) {
                setLandlord(lastBidSeat);
                result.landlordSeat = landlordSeat;
                result.bottomCards = bottomCards.clone();
                result.currentSeat = currentSeat;
            } else {
                deal();
                result.redeal = true;
                result.nextBidSeat = currentSeat;
            }
        } else {
            currentSeat = (currentSeat + 1) % playerCount;
            result.nextBidSeat = currentSeat;
        }
        
        return result;
    }
    
    private void setLandlord(int seat) {
        landlordSeat = seat;
        currentSeat = seat;
        status = 1;
        passCount = 0;
        lastCards = new int[0];
        lastPlaySeat = -1;
        
        int count = playerCardCount[seat];
        for (int i = 0; i < bottomCount; i++) {
            playerCards[seat][count + i] = bottomCards[i];
        }
        playerCardCount[seat] = count + bottomCount;
        sortCards(playerCards[seat], playerCardCount[seat]);
    }
    
    // ==================== 出牌 ====================
    
    /**
     * 出牌
     */
    public DdzPlayVO play(int seatIndex, int[] cards) {
        if (status != 1) return null;
        if (seatIndex != currentSeat) return null;
        if (cards == null || cards.length == 0) return null;
        
        if (!hasCards(seatIndex, cards)) return null;
        
        CardType type = CardRule.getType(cards);
        if (type == null) return null;
        
        if (lastCards.length > 0 && lastPlaySeat != currentSeat) {
            if (!CardRule.canBeat(lastCards, cards)) return null;
        }
        
        removeCards(seatIndex, cards);
        
        lastCards = cards.clone();
        lastPlaySeat = currentSeat;
        passCount = 0;
        
        if (type == CardType.BOMB || type == CardType.JOKER_BOMB) {
            multiple *= 2;
        }
        
        DdzPlayVO result = new DdzPlayVO();
        result.seat = currentSeat;
        result.cards = cards;
        result.type = type.name();
        result.remainCount = playerCardCount[seatIndex];
        result.pass = false;
        
        if (playerCardCount[seatIndex] == 0) {
            result.gameOver = true;
            result.winner = currentSeat;
            result.landlordWin = currentSeat == landlordSeat;
            result.multiple = multiple;
        } else {
            currentSeat = (currentSeat + 1) % playerCount;
            result.nextSeat = currentSeat;
        }
        
        return result;
    }
    
    /**
     * 不出
     */
    public DdzPlayVO pass(int seatIndex) {
        if (status != 1) return null;
        if (seatIndex != currentSeat) return null;
        
        if (lastCards.length == 0 || lastPlaySeat == currentSeat) return null;
        
        passCount++;
        int prevSeat = currentSeat;
        currentSeat = (currentSeat + 1) % playerCount;
        
        DdzPlayVO result = new DdzPlayVO();
        result.seat = prevSeat;
        result.pass = true;
        result.nextSeat = currentSeat;
        
        if (passCount >= playerCount - 1) {
            lastCards = new int[0];
            lastPlaySeat = -1;
            passCount = 0;
            result.clearLast = true;
        }
        
        return result;
    }
    
    // ==================== 工具方法 ====================
    
    private boolean hasCards(int seatIndex, int[] cards) {
        int[] hand = playerCards[seatIndex];
        int count = playerCardCount[seatIndex];
        
        int[] handCount = new int[54];
        for (int i = 0; i < count; i++) {
            handCount[hand[i]]++;
        }
        
        for (int card : cards) {
            if (handCount[card] <= 0) return false;
            handCount[card]--;
        }
        
        return true;
    }
    
    private void removeCards(int seatIndex, int[] cards) {
        int[] hand = playerCards[seatIndex];
        int count = playerCardCount[seatIndex];
        
        for (int card : cards) {
            for (int i = 0; i < count; i++) {
                if (hand[i] == card) {
                    hand[i] = hand[count - 1];
                    count--;
                    break;
                }
            }
        }
        
        playerCardCount[seatIndex] = count;
        sortCards(hand, count);
    }
    
    private int[] createDeck(int deckCount) {
        int[] deck = new int[54 * deckCount];
        int idx = 0;
        for (int d = 0; d < deckCount; d++) {
            for (int i = 0; i < 54; i++) {
                deck[idx++] = i;
            }
        }
        return deck;
    }
    
    private void shuffle(int[] deck) {
        Random random = new Random();
        for (int i = deck.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = deck[i];
            deck[i] = deck[j];
            deck[j] = temp;
        }
    }
    
    private void sortCards(int[] cards, int count) {
        for (int i = 0; i < count - 1; i++) {
            for (int j = 0; j < count - 1 - i; j++) {
                if (getCardValue(cards[j]) < getCardValue(cards[j + 1])) {
                    int temp = cards[j];
                    cards[j] = cards[j + 1];
                    cards[j + 1] = temp;
                }
            }
        }
    }
    
    public static int getCardValue(int card) {
        if (card == 52) return 13;
        if (card == 53) return 14;
        return card % 13;
    }
}
