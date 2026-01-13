package game.hall.model;

/**
 * 等待中的房间 (用于快速开始)
 */
public class WaitingRoom {
    public String roomId;
    public String serverId;
    public String serverIp;
    public int serverPort;
    public int httpPort;
    public int currentPlayers;
    public int maxPlayers;
    public String gameType;
    public String level;
    public long createTime;
}
