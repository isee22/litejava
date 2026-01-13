package game.mahjong.vo;

/**
 * 杠牌结果
 */
public class MjGangResultVO {
    /** 杠牌玩家座位 */
    public int seatIndex;
    /** 杠的牌 */
    public int card;
    /** 杠类型: angang(暗杠), minggang(明杠), bugang(补杠) */
    public String type;
    /** 杠后摸的牌 (-1 表示没摸到) */
    public int drawn = -1;
}
