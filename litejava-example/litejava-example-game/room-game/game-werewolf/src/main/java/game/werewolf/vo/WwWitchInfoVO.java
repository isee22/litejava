package game.werewolf.vo;

import java.util.List;

/**
 * 女巫行动信息
 */
public class WwWitchInfoVO extends WwTurnVO {
    /** 今晚被杀的座位 (-1表示无人被杀) */
    public int killedSeat;
    /** 是否还有解药 */
    public boolean hasAntidote;
    /** 是否还有毒药 */
    public boolean hasPoison;
    /** 毒药可选目标 */
    public List<Integer> poisonTargets;
}
