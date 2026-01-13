package game.doudizhu.vo;

/**
 * 出牌结果
 */
public class DdzPlayResultVO {
    public int seatIndex;
    public int[] cards;
    public boolean pass;
    public int nextSeat;
    public int remainCards;
    public boolean gameOver;
    public int winner;
    public boolean clearLast;  // 清空上一手牌（两人都 pass 后）
}
