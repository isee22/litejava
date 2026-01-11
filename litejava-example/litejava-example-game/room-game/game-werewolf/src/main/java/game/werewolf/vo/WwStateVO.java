package game.werewolf.vo;

import java.util.List;

/**
 * 游戏状态 (断线重连)
 */
public class WwStateVO {
    /** 当前天数 */
    public int day;
    /** 当前阶段 */
    public int phase;
    /** 我的角色 */
    public int myRole;
    /** 存活状态 */
    public boolean[] alive;
    /** 狼人同伴 (仅狼人可见) */
    public List<Integer> wolfSeats;
}
