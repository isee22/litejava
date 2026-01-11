package game.common.vo;

/**
 * 登录响应 VO
 */
public class LoginRespVO {
    public long userId;
    public String name;
    public int coins;
    public int diamonds;
    public int level;
    public int vipLevel;
    public int signDay;
    public boolean todaySigned;
    public ReconnectVO reconnect;
    
    public static class ReconnectVO {
        public String gameType;
        public String serverId;
        public String roomId;
    }
}
