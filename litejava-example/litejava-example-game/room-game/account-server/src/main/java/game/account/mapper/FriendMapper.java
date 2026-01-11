package game.account.mapper;

import game.account.entity.FriendEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendMapper {
    
    @Select("SELECT * FROM friend WHERE user_id = #{userId}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "friendId", column = "friend_id"),
        @Result(property = "createTime", column = "create_time")
    })
    List<FriendEntity> findByUserId(long userId);
    
    @Select("SELECT * FROM friend WHERE user_id = #{userId} AND friend_id = #{friendId}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "friendId", column = "friend_id"),
        @Result(property = "createTime", column = "create_time")
    })
    FriendEntity findByUserAndFriend(@Param("userId") long userId, @Param("friendId") long friendId);
    
    @Insert("INSERT INTO friend (user_id, friend_id, create_time) VALUES (#{userId}, #{friendId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FriendEntity entity);
    
    @Delete("DELETE FROM friend WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int delete(@Param("userId") long userId, @Param("friendId") long friendId);
    
    @Select("SELECT COUNT(*) FROM friend WHERE user_id = #{userId}")
    int countByUserId(long userId);
}
