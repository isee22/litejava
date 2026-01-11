package game.common.vo;

import java.util.List;

/**
 * 匹配成功回调（Match -> Gateway）
 */
public class MatchCallbackVO {
    public long userId;
    public String gameType;
    public String roomLevel;
    public String roomId;
    public String serverId;
    public String wsUrl;
    public List<Long> players;
    public List<String> names;
}
