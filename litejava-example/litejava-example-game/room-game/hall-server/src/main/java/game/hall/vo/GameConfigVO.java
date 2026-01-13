package game.hall.vo;

/**
 * 游戏配置 VO (从缓存读取，由 AccountServer 同步)
 */
public class GameConfigVO {
    public int id;
    public String gameType;
    public int roomLevel;
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
    public int status;
    public int sortOrder;
    public long createTime;
    public long updateTime;
}
