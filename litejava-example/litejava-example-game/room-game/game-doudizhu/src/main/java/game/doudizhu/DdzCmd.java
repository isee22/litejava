package game.doudizhu;

/**
 * 斗地主命令号 (1000+)
 */
public final class DdzCmd {
    
    private DdzCmd() {}
    
    public static final int BID = 1001;           // 叫地主
    public static final int BID_RESULT = 1002;
    public static final int PLAY = 1003;          // 出牌
    public static final int PLAY_RESULT = 1004;
    public static final int PASS = 1005;          // 不出
}
