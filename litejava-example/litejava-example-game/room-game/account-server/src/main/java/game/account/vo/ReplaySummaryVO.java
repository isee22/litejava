package game.account.vo;

import java.util.List;

public class ReplaySummaryVO {
    public String replayId;
    public String gameType;
    public long startTime;
    public long endTime;
    public List<String> playerNames;
    public int winner;
    public int actionCount;
}
