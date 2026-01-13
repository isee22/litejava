package game.mahjong.vo;

import java.util.List;

/**
 * 出牌结果
 */
public class MjDiscardResultVO {
    /** 出牌玩家座位 */
    public int seatIndex;
    /** 出的牌 */
    public int card;
    /** 其他玩家可执行的操作 */
    public List<MjActionVO> actions;
    /** 下一个操作玩家座位 (-1 表示等待其他玩家操作) */
    public int nextSeat = -1;
}
