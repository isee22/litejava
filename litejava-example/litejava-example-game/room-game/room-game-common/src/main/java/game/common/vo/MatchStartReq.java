package game.common.vo;

/**
 * 开始匹配请求
 */
public class MatchStartReq {
    public long userId;
    public String name;
    public String gameType;
    public String roomLevel = "beginner";
    public int coins;
}
