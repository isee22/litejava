package game.texas;

/**
 * 德州扑克命令号 (3000+)
 */
public final class TxCmd {
    
    private TxCmd() {}
    
    public static final int CALL = 3001;           // 跟注
    public static final int RAISE = 3002;          // 加注
    public static final int CHECK = 3003;          // 过牌
    public static final int FOLD = 3004;           // 弃牌
    public static final int ALLIN = 3005;          // 全下
    public static final int ACTION_RESULT = 3006;
    public static final int COMMUNITY = 3010;      // 公共牌
    public static final int SHOWDOWN = 3011;       // 摊牌
}
