package game.mahjong.vo;

import java.util.List;

/**
 * 胡牌结果
 */
public class MjHuResultVO {
    /** 胡牌玩家座位 */
    public int seatIndex;
    /** 是否自摸 */
    public boolean selfDrawn;
    /** 胡牌时的手牌 */
    public List<Integer> hand;
    /** 游戏是否结束 */
    public boolean gameOver;
    /** 所有胡牌玩家 (血战到底模式) */
    public List<Integer> winners;
    /** 下一个操作玩家座位 (游戏未结束时) */
    public int nextSeat = -1;
}
