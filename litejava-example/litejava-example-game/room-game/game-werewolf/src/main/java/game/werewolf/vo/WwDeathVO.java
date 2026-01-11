package game.werewolf.vo;

import java.util.List;

/**
 * 死亡通知
 */
public class WwDeathVO {
    /** 死亡玩家座位列表 */
    public List<Integer> deadSeats;
    /** 死亡原因 */
    public String reason;
}
