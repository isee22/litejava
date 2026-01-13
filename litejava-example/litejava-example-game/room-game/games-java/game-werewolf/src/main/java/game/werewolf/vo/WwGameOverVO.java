package game.werewolf.vo;

import java.util.Map;

/**
 * 游戏结束通知
 */
public class WwGameOverVO {
    /** 胜利方 (1=好人, 2=狼人) */
    public int winner;
    /** 胜利方名称 */
    public String winnerName;
    /** 所有玩家角色 [seatIndex] -> role */
    public Map<Integer, Integer> roles;
}
