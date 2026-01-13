package game.account.mapper;

import game.account.entity.GameReplayEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 游戏录像 Mapper
 */
@Mapper
public interface GameReplayMapper {
    
    @Insert("INSERT INTO game_replay (replayId, gameType, roomId, players, playerNames, winner, initState, actions, actionCount, startTime, endTime) " +
            "VALUES (#{replayId}, #{gameType}, #{roomId}, #{players}, #{playerNames}, #{winner}, #{initState}, #{actions}, #{actionCount}, #{startTime}, #{endTime})")
    int insert(GameReplayEntity entity);
    
    @Update("UPDATE game_replay SET winner=#{winner}, actions=#{actions}, actionCount=#{actionCount}, endTime=#{endTime} WHERE replayId=#{replayId}")
    int update(GameReplayEntity entity);
    
    @Select("SELECT * FROM game_replay WHERE replayId=#{replayId}")
    GameReplayEntity findById(@Param("replayId") String replayId);
    
    @Select("SELECT r.replayId, r.gameType, r.roomId, r.players, r.playerNames, " +
            "r.winner, r.actionCount, r.startTime, r.endTime " +
            "FROM user_replay ur JOIN game_replay r ON ur.replayId = r.replayId " +
            "WHERE ur.userId=#{userId} ORDER BY ur.createTime DESC LIMIT #{limit}")
    List<GameReplayEntity> findByUserId(@Param("userId") long userId, @Param("limit") int limit);
    
    @Insert("INSERT IGNORE INTO user_replay (userId, replayId, createTime) VALUES (#{userId}, #{replayId}, #{createTime})")
    int insertUserReplay(@Param("userId") long userId, @Param("replayId") String replayId, @Param("createTime") long createTime);
    
    @Select("SELECT COUNT(*) FROM game_replay")
    int count();
    
    @Delete("DELETE FROM game_replay WHERE startTime < #{beforeTime}")
    int deleteOldReplays(@Param("beforeTime") long beforeTime);
    
    @Delete("DELETE FROM user_replay WHERE createTime < #{beforeTime}")
    int deleteOldUserReplays(@Param("beforeTime") long beforeTime);
}
