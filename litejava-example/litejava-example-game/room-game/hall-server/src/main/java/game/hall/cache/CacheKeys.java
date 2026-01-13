package game.hall.cache;

/**
 * Redis 缓存 Key 统一定义
 * 
 * 所有 HallServer 使用的 Redis Key 都在这里定义，
 * 便于维护和避免冲突
 */
public class CacheKeys {
    
    // ==================== GameServer 注册 ====================
    
    /** GameServer 信息: hall:gs:{serverId} -> ServerInfo JSON */
    public static String server(String serverId) {
        return "hall:gs:" + serverId;
    }
    
    /** GameServer 列表 (Set): hall:gs:list -> {serverId1, serverId2, ...} */
    public static final String SERVER_LIST = "hall:gs:list";
    
    /** GameServer 过期时间 (秒) */
    public static final int SERVER_EXPIRE = 120;
    
    // ==================== 房间映射 ====================
    
    /** 房间信息: hall:room:{roomId} -> RoomInfo JSON */
    public static String room(String roomId) {
        return "hall:room:" + roomId;
    }
    
    /** 服务器的房间索引 (Set): hall:rooms:{serverId} -> {roomId1, roomId2, ...} */
    public static String serverRooms(String serverId) {
        return "hall:rooms:" + serverId;
    }
    
    /** 可加入房间列表 (Set): hall:joinable:{gameType} -> {roomId1, roomId2, ...} */
    public static String joinableRooms(String gameType) {
        return "hall:joinable:" + gameType;
    }
    
    /** 房间过期时间 (秒) - 24小时 */
    public static final int ROOM_EXPIRE = 86400;
    
    // ==================== 用户状态 ====================
    
    /** 玩家信息 (AccountServer 缓存): player:{userId} -> Player JSON */
    public static String player(long userId) {
        return "player:" + userId;
    }
    
    /** 用户当前房间: hall:user:{userId} -> roomId */
    public static String user(long userId) {
        return "hall:user:" + userId;
    }
    
    /** 房间内用户索引 (Set): hall:room:users:{roomId} -> {userId1, userId2, ...} */
    public static String roomUsers(String roomId) {
        return "hall:room:users:" + roomId;
    }
    
    // ==================== 快速开始 ====================
    
    /** 可加入的房间 (按级别): hall:avail:{gameType}:{roomLevel} -> roomId */
    public static String available(String gameType, int roomLevel) {
        return "hall:avail:" + gameType + ":" + roomLevel;
    }
    
    // ==================== 匹配队列 ====================
    
    /** 匹配队列 (Sorted Set): hall:match:{gameType} -> {userId: score(timestamp)} */
    public static String matchQueue(String gameType) {
        return "hall:match:" + gameType;
    }
    
    /** 匹配用户信息: hall:match:user:{userId} -> MatchUserInfo JSON */
    public static String matchUser(long userId) {
        return "hall:match:user:" + userId;
    }
    
    /** 匹配过期时间 (秒) - 5分钟 */
    public static final int MATCH_EXPIRE = 300;
    
    // ==================== 分布式锁 ====================
    
    /** 分布式锁: hall:lock:{name} */
    public static String lock(String name) {
        return "hall:lock:" + name;
    }
    
    /** 快速开始锁 (防止同时加入同一房间) */
    public static String lockQuickStart(String gameType) {
        return "hall:lock:qs:" + gameType;
    }
    
    /** 匹配锁 */
    public static String lockMatch(String gameType) {
        return "hall:lock:match:" + gameType;
    }
    
    /** 锁过期时间 (秒) */
    public static final int LOCK_EXPIRE = 5;
}
