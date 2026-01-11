package game.account.mapper;

import game.account.entity.PlayerEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlayerMapper {
    
    @Select("SELECT * FROM player WHERE userId = #{userId}")
    PlayerEntity findById(@Param("userId") long userId);
    
    @Select("SELECT * FROM player ORDER BY coins DESC LIMIT 1000")
    List<PlayerEntity> findAll();
    
    @Insert("INSERT INTO player (userId, name, avatar, sex, coins, diamonds, level, exp, vipLevel, vipExp, totalGames, winGames, loginDays, createTime, lastLoginTime) " +
            "VALUES (#{userId}, #{name}, #{avatar}, #{sex}, #{coins}, #{diamonds}, #{level}, #{exp}, #{vipLevel}, #{vipExp}, #{totalGames}, #{winGames}, #{loginDays}, #{createTime}, #{lastLoginTime})")
    int insert(PlayerEntity entity);
    
    @Update("UPDATE player SET name=#{name}, avatar=#{avatar}, sex=#{sex}, coins=#{coins}, diamonds=#{diamonds}, level=#{level}, exp=#{exp}, " +
            "vipLevel=#{vipLevel}, vipExp=#{vipExp}, totalGames=#{totalGames}, winGames=#{winGames}, loginDays=#{loginDays}, lastLoginTime=#{lastLoginTime} " +
            "WHERE userId = #{userId}")
    int update(PlayerEntity entity);
}
