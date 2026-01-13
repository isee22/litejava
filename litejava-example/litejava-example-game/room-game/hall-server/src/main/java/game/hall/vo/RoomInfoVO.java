package game.hall.vo;

/**
 * 房间信息 (存储在 Redis)
 */
public class RoomInfoVO {
    
    public String roomId;
    public String serverId;
    public String gameType;
    public long ownerId;
    public int playerCount;
    public int maxPlayers;
    public boolean joinable;
    public long createTime;
}
