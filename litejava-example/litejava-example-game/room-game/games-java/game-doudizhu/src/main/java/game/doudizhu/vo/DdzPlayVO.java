package game.doudizhu.vo;

/**
 * 出牌结果 (Game层返回)
 */
public class DdzPlayVO {
    /** 出牌座位 */
    public int seat;
    /** 出的牌 */
    public int[] cards;
    /** 牌型名称 */
    public String type;
    /** 剩余手牌数 */
    public int remainCount;
    /** 是否不出 */
    public boolean pass;
    /** 下一个出牌座位 */
    public int nextSeat = -1;
    /** 是否清空上一手 (所有人都不出时) */
    public boolean clearLast;
    /** 游戏是否结束 */
    public boolean gameOver;
    /** 获胜座位 */
    public int winner = -1;
    /** 地主是否获胜 */
    public boolean landlordWin;
    /** 倍数 */
    public int multiple = 1;
}
