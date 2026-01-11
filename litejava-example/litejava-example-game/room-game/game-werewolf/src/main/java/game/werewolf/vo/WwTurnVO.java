package game.werewolf.vo;

import java.util.List;

/**
 * 轮到行动通知
 */
public class WwTurnVO {
    /** 行动类型: kill/check/save/poison/protect/speak/vote/shoot */
    public String action;
    /** 可选目标座位列表 */
    public List<Integer> targets;
    /** 超时时间(秒) */
    public int timeout;
}
