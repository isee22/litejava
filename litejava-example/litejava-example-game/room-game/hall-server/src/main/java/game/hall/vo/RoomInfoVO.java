package game.hall.vo;

/**
 * 房间信息 (存储在 Redis)
 */
public class RoomInfoVO {
    
    public String roomId;
    public String serverId;
    public String gameType;
    public int roomLevel;      // 房间级别 (用于匹配同级别玩家)
    public long ownerId;
    public int playerCount;
    public int maxPlayers;
    public boolean joinable;
    public long createTime;
}
