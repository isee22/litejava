package game.niuniu;

/**
 * 牛牛错误码 (400+)
 */
public final class NnErr {
    
    private NnErr() {}
    
    public static final int ALREADY_BET = 400;
    public static final int BANKER_CANNOT_BET = 401;
    public static final int NOT_BET_PHASE = 402;
    public static final int NOT_SHOW_PHASE = 403;
}
