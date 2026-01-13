package game.account.mapper;

import game.account.entity.Player;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlayerMapper {
    
    @Select("SELECT * FROM player WHERE userId = #{userId}")
    Player findById(@Param("userId") long userId);
    
    @Select("SELECT * FROM player ORDER BY coins DESC LIMIT 1000")
    List<Player> findAll();
    
    @Insert("INSERT INTO player (userId, name, avatar, sex, coins, diamonds, level, exp, vipLevel, vipExp, totalGames, winGames, escapeGames, creditScore, loginDays, createTime, lastLoginTime) " +
            "VALUES (#{userId}, #{name}, #{avatar}, #{sex}, #{coins}, #{diamonds}, #{level}, #{exp}, #{vipLevel}, #{vipExp}, #{totalGames}, #{winGames}, #{escapeGames}, #{creditScore}, #{loginDays}, #{createTime}, #{lastLoginTime})")
    int insert(Player entity);
    
    @Update("UPDATE player SET name=#{name}, avatar=#{avatar}, sex=#{sex}, coins=#{coins}, diamonds=#{diamonds}, level=#{level}, exp=#{exp}, " +
            "vipLevel=#{vipLevel}, vipExp=#{vipExp}, totalGames=#{totalGames}, winGames=#{winGames}, escapeGames=#{escapeGames}, creditScore=#{creditScore}, loginDays=#{loginDays}, lastLoginTime=#{lastLoginTime} " +
            "WHERE userId = #{userId}")
    int update(Player entity);
}

