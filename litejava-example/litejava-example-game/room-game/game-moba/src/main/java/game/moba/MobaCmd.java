package game.moba;

/**
 * MOBA命令号 (6000+)
 */
public final class MobaCmd {
    
    private MobaCmd() {}
    
    public static final int MOVE = 6001;           // 移动
    public static final int ATTACK = 6002;         // 攻击
    public static final int SKILL = 6003;          // 技能
    public static final int ATTACK_BASE = 6004;    // 攻击基地
    public static final int FRAME = 6010;          // 帧同步
}
