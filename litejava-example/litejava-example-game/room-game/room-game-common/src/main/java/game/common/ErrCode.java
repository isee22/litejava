package game.common;

/**
 * 公共错误码 (1-99)
 * 
 * 100+ 由各游戏自定义，如:
 * - game.doudizhu.DdzErr (100+)
 * - game.mahjong.MjErr (100+)
 */
public final class ErrCode {
    
    private ErrCode() {}
    
    // ==================== 系统错误 (1-19) ====================
    public static final int OK = 0;
    public static final int UNKNOWN = 1;
    public static final int NOT_LOGIN = 2;
    public static final int NOT_IN_ROOM = 3;
    public static final int ALREADY_MATCHING = 4;
    public static final int NO_SERVER = 5;
    public static final int KICKED = 6;  // 被踢下线（账号在其他地方登录）
    public static final int INVALID_TOKEN = 7;  // Token 无效或过期
    
    // ==================== 房间错误 (20-39) ====================
    public static final int ROOM_NOT_FOUND = 20;
    public static final int ROOM_FULL = 21;
    public static final int ROOM_NOT_IN = 22;
    public static final int ROOM_ALREADY_IN = 23;
    
    // ==================== 游戏通用错误 (40-99) ====================
    public static final int GAME_NOT_STARTED = 40;
    public static final int GAME_ALREADY_STARTED = 41;
    public static final int GAME_OVER = 42;
    public static final int NOT_YOUR_TURN = 43;
    public static final int INVALID_ACTION = 44;
    public static final int INVALID_CARDS = 45;
    public static final int CANNOT_PASS = 46;
    public static final int CANNOT_BEAT = 47;
}
