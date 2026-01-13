package game.robot;

/**
 * 协议命令号 (机器人本地定义，不依赖游戏模块)
 * 
 * 范围:
 * - 1-99: 系统
 * - 100-499: 房间/聊天
 * - 500-999: 游戏通用
 * - 1000+: 各游戏自定义
 */
public final class Cmd {
    
    private Cmd() {}
    
    // ==================== 系统 (1-99) ====================
    public static final int LOGIN = 1;
    public static final int PING = 4;
    
    // ==================== 房间 (100-199) ====================
    public static final int ROOM_EXIT = 106;
    
    // ==================== 聊天 (150-199) ====================
    public static final int CHAT_MSG = 151;
    
    // ==================== 游戏通用 (500-599) ====================
    public static final int USER_JOIN = 500;
    public static final int USER_EXIT = 501;
    public static final int USER_STATE = 502;
    public static final int USER_READY = 503;
    public static final int READY = 504;
    public static final int GAME_START = 510;
    public static final int GAME_OVER = 511;
    public static final int GAME_STATE = 512;
    public static final int DEAL = 520;
    public static final int DRAW = 521;
    public static final int TURN = 522;
    
    // ==================== 斗地主 (1000+) ====================
    public static final int DDZ_BID = 1001;
    public static final int DDZ_BID_RESULT = 1002;
    public static final int DDZ_PLAY = 1003;
    public static final int DDZ_PLAY_RESULT = 1004;
    public static final int DDZ_PASS = 1005;
    
    // ==================== 麻将 (2000+) ====================
    public static final int MJ_DISCARD = 2001;
    public static final int MJ_DISCARD_RESULT = 2002;
    public static final int MJ_PENG = 2003;
    public static final int MJ_PENG_RESULT = 2004;
    public static final int MJ_GANG = 2005;
    public static final int MJ_GANG_RESULT = 2006;
    public static final int MJ_HU = 2007;
    public static final int MJ_HU_RESULT = 2008;
    public static final int MJ_PASS = 2009;
    public static final int MJ_ACTION = 2010;
    
    // ==================== 德州扑克 (3000+) ====================
    public static final int TX_CALL = 3001;
    public static final int TX_RAISE = 3002;
    public static final int TX_CHECK = 3003;
    public static final int TX_FOLD = 3004;
    public static final int TX_ALLIN = 3005;
    public static final int TX_ACTION_RESULT = 3006;
    public static final int TX_COMMUNITY = 3010;
    public static final int TX_SHOWDOWN = 3011;
    
    // ==================== 牛牛 (4000+) ====================
    public static final int NN_BET = 4001;
    public static final int NN_BET_RESULT = 4002;
    public static final int NN_SHOW = 4003;
    public static final int NN_SHOW_RESULT = 4004;
    public static final int NN_FIFTH_CARD = 4005;
    public static final int NN_SETTLE = 4006;
    
    // ==================== 五子棋 (5000+) ====================
    public static final int GB_MOVE = 5001;
    public static final int GB_MOVE_RESULT = 5002;
}
