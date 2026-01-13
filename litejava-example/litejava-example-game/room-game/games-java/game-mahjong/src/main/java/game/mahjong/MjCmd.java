package game.mahjong;

/**
 * 麻将命令号 (2000+)
 */
public final class MjCmd {
    
    private MjCmd() {}
    
    public static final int DISCARD = 2001;        // 出牌
    public static final int DISCARD_RESULT = 2002;
    public static final int PENG = 2003;           // 碰
    public static final int PENG_RESULT = 2004;
    public static final int GANG = 2005;           // 杠
    public static final int GANG_RESULT = 2006;
    public static final int HU = 2007;             // 胡
    public static final int HU_RESULT = 2008;
    public static final int PASS = 2009;           // 过
    public static final int ACTION = 2010;         // 可执行操作提示
}
