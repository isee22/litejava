package game.account.mapper;

import game.account.entity.FriendRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    
    @Select("SELECT * FROM friend_request WHERE toId = #{toId} AND status = 0 ORDER BY createTime DESC")
    List<FriendRequest> findPendingByToId(long toId);
    
    @Select("SELECT * FROM friend_request WHERE fromId = #{fromId} AND toId = #{toId} AND status = 0")
    FriendRequest findPending(@Param("fromId") long fromId, @Param("toId") long toId);
    
    @Select("SELECT * FROM friend_request WHERE id = #{id}")
    FriendRequest findById(long id);
    
    @Insert("INSERT INTO friend_request (fromId, toId, message, status, createTime, updateTime) " +
            "VALUES (#{fromId}, #{toId}, #{message}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FriendRequest entity);
    
    @Update("UPDATE friend_request SET status = #{status}, updateTime = #{updateTime} WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") int status, @Param("updateTime") long updateTime);
}
