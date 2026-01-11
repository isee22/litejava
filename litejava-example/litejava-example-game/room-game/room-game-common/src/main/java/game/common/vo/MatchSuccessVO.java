package game.common.vo;

import java.util.List;

/**
 * 匹配成功通知
 */
public class MatchSuccessVO {
    public String gameType;
    public String roomLevel;
    public String roomId;
    public String serverId;
    public String host;
    public int wsPort;
    public String wsUrl;
    public List<Long> players;
    public List<String> names;
}
