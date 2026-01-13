package game.account.mapper;

import game.account.entity.GameConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 房间配置 Mapper
 */
@Mapper
public interface GameConfigMapper {
    
    @Select("SELECT * FROM room_config WHERE id = #{id}")
    GameConfig findById(int id);
    
    @Select("SELECT * FROM room_config WHERE gameType = #{gameType} AND status = 1 ORDER BY sortOrder")
    List<GameConfig> findByGameType(String gameType);
    
    @Select("SELECT * FROM room_config WHERE gameType = #{gameType} AND roomLevel = #{roomLevel}")
    GameConfig findByTypeAndLevel(@Param("gameType") String gameType, @Param("roomLevel") int roomLevel);
    
    @Select("SELECT * FROM room_config WHERE status = 1 ORDER BY gameType, sortOrder")
    List<GameConfig> findAllEnabled();
    
    @Insert("INSERT INTO room_config (gameType, roomLevel, roomName, minPlayers, maxPlayers, " +
            "minCoins, maxCoins, baseScore, initChips, roundTime, matchTimeout, extraConfig, " +
            "status, sortOrder, createTime, updateTime) VALUES (#{gameType}, #{roomLevel}, #{roomName}, " +
            "#{minPlayers}, #{maxPlayers}, #{minCoins}, #{maxCoins}, #{baseScore}, #{initChips}, " +
            "#{roundTime}, #{matchTimeout}, #{extraConfig}, #{status}, #{sortOrder}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GameConfig config);
    
    @Update("UPDATE room_config SET roomName=#{roomName}, minPlayers=#{minPlayers}, " +
            "maxPlayers=#{maxPlayers}, minCoins=#{minCoins}, maxCoins=#{maxCoins}, " +
            "baseScore=#{baseScore}, initChips=#{initChips}, roundTime=#{roundTime}, " +
            "matchTimeout=#{matchTimeout}, extraConfig=#{extraConfig}, status=#{status}, " +
            "sortOrder=#{sortOrder}, updateTime=#{updateTime} WHERE id=#{id}")
    int update(GameConfig config);
}
