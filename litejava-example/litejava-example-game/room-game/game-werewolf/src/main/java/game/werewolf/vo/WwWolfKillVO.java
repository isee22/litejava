package game.werewolf.vo;

/**
 * 狼人杀人结果
 */
public class WwWolfKillVO {
    /** 投票的狼人座位 */
    public int wolfSeat;
    /** 目标座位 */
    public int targetSeat;
    /** 是否已确认 (所有狼人投票完成) */
    public boolean confirmed;
    /** 最终目标 (confirmed=true时有效) */
    public int finalTarget = -1;
}
