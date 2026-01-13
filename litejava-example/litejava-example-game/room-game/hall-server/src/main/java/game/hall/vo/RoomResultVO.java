package game.hall.vo;

/**
 * 房间操作结果 VO
 */
public class RoomResultVO {
    public String roomId;
    public String token;
    public String wsUrl;
    public String ip;
    public int port;
    public String gameType;
    
    // 私人房额外字段
    public long time;
    public String sign;
    
    /** 是否为重连 */
    public boolean reconnect;
}
