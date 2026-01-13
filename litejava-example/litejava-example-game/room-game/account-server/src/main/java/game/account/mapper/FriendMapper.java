package game.account.mapper;

import game.account.entity.Friend;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendMapper {
    
    @Select("SELECT * FROM friend WHERE userId = #{userId}")
    List<Friend> findByUserId(long userId);
    
    @Select("SELECT * FROM friend WHERE userId = #{userId} AND friendId = #{friendId}")
    Friend findByUserAndFriend(@Param("userId") long userId, @Param("friendId") long friendId);
    
    @Insert("INSERT INTO friend (userId, friendId, createTime) VALUES (#{userId}, #{friendId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Friend entity);
    
    @Delete("DELETE FROM friend WHERE userId = #{userId} AND friendId = #{friendId}")
    int delete(@Param("userId") long userId, @Param("friendId") long friendId);
    
    @Select("SELECT COUNT(*) FROM friend WHERE userId = #{userId}")
    int countByUserId(long userId);
}
