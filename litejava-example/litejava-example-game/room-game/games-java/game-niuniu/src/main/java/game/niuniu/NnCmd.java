package game.niuniu;

/**
 * 牛牛命令号 (4000+)
 */
public final class NnCmd {
    
    private NnCmd() {}
    
    public static final int BET = 4001;            // 下注
    public static final int BET_RESULT = 4002;
    public static final int SHOW = 4003;           // 亮牌
    public static final int SHOW_RESULT = 4004;
    public static final int FIFTH_CARD = 4005;     // 第5张牌
    public static final int SETTLE = 4006;         // 结算
}
