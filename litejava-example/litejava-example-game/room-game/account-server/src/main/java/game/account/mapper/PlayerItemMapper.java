package game.account.mapper;

import game.account.entity.PlayerItemEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlayerItemMapper {
    
    @Select("SELECT id, user_id as userId, item_id as itemId, count, " +
            "expire_time as expireTime, create_time as createTime, update_time as updateTime " +
            "FROM player_item WHERE user_id = #{userId}")
    List<PlayerItemEntity> findByUserId(long userId);
    
    @Select("SELECT id, user_id as userId, item_id as itemId, count, " +
            "expire_time as expireTime, create_time as createTime, update_time as updateTime " +
            "FROM player_item WHERE user_id = #{userId} AND item_id = #{itemId}")
    PlayerItemEntity findByUserAndItem(@Param("userId") long userId, @Param("itemId") int itemId);
    
    @Insert("INSERT INTO player_item (user_id, item_id, count, expire_time, create_time, update_time) " +
            "VALUES (#{userId}, #{itemId}, #{count}, #{expireTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlayerItemEntity entity);
    
    @Update("UPDATE player_item SET count = #{count}, expire_time = #{expireTime}, " +
            "update_time = #{updateTime} WHERE id = #{id}")
    int update(PlayerItemEntity entity);
    
    @Delete("DELETE FROM player_item WHERE id = #{id}")
    int delete(long id);
    
    @Delete("DELETE FROM player_item WHERE user_id = #{userId} AND item_id = #{itemId} AND count <= 0")
    int deleteEmpty(@Param("userId") long userId, @Param("itemId") int itemId);
}
