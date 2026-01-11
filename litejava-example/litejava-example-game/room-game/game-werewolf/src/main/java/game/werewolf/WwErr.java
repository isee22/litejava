package game.werewolf;

/**
 * 狼人杀错误码定义
 * 
 * 错误码范围: 700-799
 */
public class WwErr {
    
    /** 不是你的回合 */
    public static final int NOT_YOUR_TURN = 701;
    
    /** 目标无效 */
    public static final int INVALID_TARGET = 702;
    
    /** 目标已死亡 */
    public static final int TARGET_DEAD = 703;
    
    /** 不是当前阶段 */
    public static final int WRONG_PHASE = 704;
    
    /** 你没有这个角色 */
    public static final int NOT_YOUR_ROLE = 705;
    
    /** 解药已用完 */
    public static final int NO_ANTIDOTE = 706;
    
    /** 毒药已用完 */
    public static final int NO_POISON = 707;
    
    /** 不能连续守护同一人 */
    public static final int SAME_GUARD_TARGET = 708;
    
    /** 已经投过票 */
    public static final int ALREADY_VOTED = 709;
    
    /** 你已死亡 */
    public static final int YOU_ARE_DEAD = 710;
    
    /** 不能自杀 */
    public static final int CANNOT_SELF_KILL = 711;
    
    /** 狼人还未统一意见 */
    public static final int WOLF_NOT_AGREED = 712;
}
