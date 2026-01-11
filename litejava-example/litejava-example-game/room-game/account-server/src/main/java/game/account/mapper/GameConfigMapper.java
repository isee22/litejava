package game.account.mapper;

import game.account.entity.GameConfigEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 房间配置 Mapper
 */
public interface GameConfigMapper {
    
    @Select("SELECT * FROM room_config WHERE id = #{id}")
    @Results(id = "roomConfigMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "gameType", column = "game_type"),
        @Result(property = "roomLevel", column = "room_level"),
        @Result(property = "roomName", column = "room_name"),
        @Result(property = "minPlayers", column = "min_players"),
        @Result(property = "maxPlayers", column = "max_players"),
        @Result(property = "minCoins", column = "min_coins"),
        @Result(property = "maxCoins", column = "max_coins"),
        @Result(property = "baseScore", column = "base_score"),
        @Result(property = "initChips", column = "init_chips"),
        @Result(property = "roundTime", column = "round_time"),
        @Result(property = "matchTimeout", column = "match_timeout"),
        @Result(property = "extraConfig", column = "extra_config"),
        @Result(property = "status", column = "status"),
        @Result(property = "sortOrder", column = "sort_order"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    GameConfigEntity findById(int id);
    
    @Select("SELECT * FROM room_config WHERE game_type = #{gameType} AND status = 1 ORDER BY sort_order")
    @ResultMap("roomConfigMap")
    List<GameConfigEntity> findByGameType(String gameType);
    
    @Select("SELECT * FROM room_config WHERE game_type = #{gameType} AND room_level = #{roomLevel}")
    @ResultMap("roomConfigMap")
    GameConfigEntity findByTypeAndLevel(@Param("gameType") String gameType, @Param("roomLevel") String roomLevel);
    
    @Select("SELECT * FROM room_config WHERE status = 1 ORDER BY game_type, sort_order")
    @ResultMap("roomConfigMap")
    List<GameConfigEntity> findAllEnabled();
    
    @Insert("INSERT INTO room_config (game_type, room_level, room_name, min_players, max_players, " +
            "min_coins, max_coins, base_score, init_chips, round_time, match_timeout, extra_config, " +
            "status, sort_order, create_time, update_time) VALUES (#{gameType}, #{roomLevel}, #{roomName}, " +
            "#{minPlayers}, #{maxPlayers}, #{minCoins}, #{maxCoins}, #{baseScore}, #{initChips}, " +
            "#{roundTime}, #{matchTimeout}, #{extraConfig}, #{status}, #{sortOrder}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GameConfigEntity config);
    
    @Update("UPDATE room_config SET room_name=#{roomName}, min_players=#{minPlayers}, " +
            "max_players=#{maxPlayers}, min_coins=#{minCoins}, max_coins=#{maxCoins}, " +
            "base_score=#{baseScore}, init_chips=#{initChips}, round_time=#{roundTime}, " +
            "match_timeout=#{matchTimeout}, extra_config=#{extraConfig}, status=#{status}, " +
            "sort_order=#{sortOrder}, update_time=#{updateTime} WHERE id=#{id}")
    int update(GameConfigEntity config);
}
