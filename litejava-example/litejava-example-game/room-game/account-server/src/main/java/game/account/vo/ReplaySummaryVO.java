package game.account.vo;

import game.account.entity.GameReplayEntity;

import java.util.Arrays;
import java.util.List;

/**
 * 录像摘要 VO
 */
public class ReplaySummaryVO {
    public String replayId;
    public String gameType;
    public String roomId;
    public List<String> playerNames;
    public int winner;
    public int actionCount;
    public long startTime;
    public long endTime;
    
    public static ReplaySummaryVO from(GameReplayEntity e) {
        ReplaySummaryVO vo = new ReplaySummaryVO();
        vo.replayId = e.replayId;
        vo.gameType = e.gameType;
        vo.roomId = e.roomId;
        vo.playerNames = Arrays.asList(e.playerNames.split(","));
        vo.winner = e.winner;
        vo.actionCount = e.actionCount;
        vo.startTime = e.startTime;
        vo.endTime = e.endTime;
        return vo;
    }
}
