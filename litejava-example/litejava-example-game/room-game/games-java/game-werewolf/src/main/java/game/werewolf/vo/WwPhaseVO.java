package game.werewolf.vo;

/**
 * 阶段变更通知
 */
public class WwPhaseVO {
    /** 阶段ID */
    public int phase;
    /** 当前天数 */
    public int day;
    /** 阶段名称 */
    public String phaseName;
    /** 当前发言者座位 (发言阶段) */
    public int currentSpeaker = -1;
}
