package game.common.vo;

/**
 * 房间配置 VO (场次配置)
 * 
 * 用于服务间传输房间配置数据
 */
public class RoomConfigVO {
    
    public int id;
    public String gameType;
    public String roomLevel;
    public String roomName;
    public int minPlayers;
    public int maxPlayers;
    public int minCoins;
    public int maxCoins;
    public int baseScore;
    public int initChips;
    public int roundTime;
    public int matchTimeout;
    public String extraConfig;
}
