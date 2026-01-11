package game.common;

/**
 * 公共协议命令号
 * 
 * 路由规则 (Gateway application.yml):
 * - lobby: 100-299 (房间、聊天等大厅功能)
 * - match: 300-499 (匹配相关)
 * - game:  500-999 (游戏操作，转发到 GameServer)
 * 
 * 1000+ 由各游戏自定义
 */
public final class Cmd {
    
    private Cmd() {}
    
    // ==================== 系统 (1-99) ====================
    public static final int LOGIN = 1;
    public static final int REGISTER = 2;
    public static final int LOGOUT = 3;
    public static final int PING = 4;
    public static final int KICK = 5;
    
    // ==================== Lobby: 房间 (100-149) ====================
    public static final int ROOM_LIST = 100;
    public static final int ROOM_CREATE = 102;
    public static final int ROOM_JOIN = 104;
    public static final int ROOM_EXIT = 106;
    public static final int ROOM_KICK = 108;
    public static final int ROOM_INFO = 110;
    
    // ==================== Lobby: 聊天 (150-199) ====================
    public static final int CHAT_SEND = 150;
    public static final int CHAT_MSG = 151;
    public static final int CHAT_JOIN_ROOM = 152;
    public static final int CHAT_LEAVE_ROOM = 154;
    public static final int CHAT_PRIVATE = 156;
    /** 全服广播 (消耗喇叭道具) */
    public static final int WORLD_CHAT_SEND = 160;
    /** 全服广播消息推送 */
    public static final int WORLD_CHAT_MSG = 161;
    
    // ==================== Lobby: 其他 (200-299) ====================
    public static final int SIGN_IN = 200;
    public static final int REPLAY_LIST = 210;
    public static final int REPLAY_GET = 212;
    
    // ==================== Match: 匹配 (300-499) ====================
    public static final int MATCH_START = 300;
    public static final int MATCH_CANCEL = 302;
    /** 服务器推送：匹配成功 */
    public static final int MATCH_SUCCESS = 305;
    
    // ==================== Game: 游戏通用 (500-599) ====================
    /** 服务器推送：有人加入房间 */
    public static final int USER_JOIN = 500;
    /** 服务器推送：有人退出房间 */
    public static final int USER_EXIT = 501;
    /** 服务器推送：玩家状态变化 */
    public static final int USER_STATE = 502;
    /** 服务器推送：玩家准备状态 */
    public static final int USER_READY = 503;
    public static final int READY = 504;
    /** 服务器推送：游戏开始 */
    public static final int GAME_START = 510;
    /** 服务器推送：游戏结束 */
    public static final int GAME_OVER = 511;
    /** 服务器推送：游戏状态（断线重连） */
    public static final int GAME_STATE = 512;
    /** 服务器推送：发牌 */
    public static final int DEAL = 520;
    /** 服务器推送：摸牌 */
    public static final int DRAW = 521;
    /** 服务器推送：轮到谁 */
    public static final int TURN = 522;
    public static final int RECONNECT = 530;
}
