package game.account.mapper;

import game.account.entity.PlayerItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlayerItemMapper {
    
    @Select("SELECT * FROM player_item WHERE userId = #{userId}")
    List<PlayerItem> findByUserId(long userId);
    
    @Select("SELECT * FROM player_item WHERE userId = #{userId} AND itemId = #{itemId}")
    PlayerItem findByUserAndItem(@Param("userId") long userId, @Param("itemId") int itemId);
    
    @Insert("INSERT INTO player_item (userId, itemId, count, expireTime, createTime, updateTime) " +
            "VALUES (#{userId}, #{itemId}, #{count}, #{expireTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlayerItem entity);
    
    @Update("UPDATE player_item SET count = #{count}, expireTime = #{expireTime}, " +
            "updateTime = #{updateTime} WHERE id = #{id}")
    int update(PlayerItem entity);
    
    @Delete("DELETE FROM player_item WHERE id = #{id}")
    int delete(long id);
    
    @Delete("DELETE FROM player_item WHERE userId = #{userId} AND itemId = #{itemId} AND count <= 0")
    int deleteEmpty(@Param("userId") long userId, @Param("itemId") int itemId);
}
