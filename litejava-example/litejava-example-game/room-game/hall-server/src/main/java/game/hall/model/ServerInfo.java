package game.hall.model;

/**
 * GameServer 信息
 */
public class ServerInfo {
    public String id;
    public String ip;
    public String clientip;
    public int clientport;
    public int httpPort;
    public int load;
    public String gameType;
    public long lastHeartbeat;
}
