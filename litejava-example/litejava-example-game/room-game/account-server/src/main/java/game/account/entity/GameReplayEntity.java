package game.account.entity;

import javax.persistence.*;

/**
 * 游戏录像实体
 */
@Entity
@Table(name = "game_replay")
public class GameReplayEntity {
    @Id
    @Column(name = "replayId")
    public String replayId;
    
    @Column(name = "gameType")
    public String gameType;
    
    @Column(name = "roomId")
    public String roomId;
    
    @Column(name = "players")
    public String players;      // 逗号分隔的玩家ID
    
    @Column(name = "playerNames")
    public String playerNames;  // 逗号分隔的玩家名字
    
    @Column(name = "winner")
    public int winner;
    
    @Column(name = "initState")
    public String initState;    // JSON
    
    @Column(name = "actions")
    public String actions;      // JSON
    
    @Column(name = "actionCount")
    public int actionCount;
    
    @Column(name = "startTime")
    public long startTime;
    
    @Column(name = "endTime")
    public long endTime;
}
