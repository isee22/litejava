package game.account.vo;

import java.util.List;
import java.util.Map;

public class ReplayDetailVO {
    public String replayId;
    public String gameType;
    public String roomId;
    public long startTime;
    public long endTime;
    public List<Long> players;
    public List<String> playerNames;
    public int winner;
    public Map<String, Object> initState;
    public List<ReplayActionVO> actions;
}

