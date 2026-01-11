package game.account.mapper;

import game.account.entity.FriendRequestEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    
    @Select("SELECT * FROM friend_request WHERE to_id = #{toId} AND status = 0 ORDER BY create_time DESC")
    @Results({
        @Result(property = "fromId", column = "from_id"),
        @Result(property = "toId", column = "to_id"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    List<FriendRequestEntity> findPendingByToId(long toId);
    
    @Select("SELECT * FROM friend_request WHERE from_id = #{fromId} AND to_id = #{toId} AND status = 0")
    @Results({
        @Result(property = "fromId", column = "from_id"),
        @Result(property = "toId", column = "to_id"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    FriendRequestEntity findPending(@Param("fromId") long fromId, @Param("toId") long toId);
    
    @Select("SELECT * FROM friend_request WHERE id = #{id}")
    @Results({
        @Result(property = "fromId", column = "from_id"),
        @Result(property = "toId", column = "to_id"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    FriendRequestEntity findById(long id);
    
    @Insert("INSERT INTO friend_request (from_id, to_id, message, status, create_time, update_time) " +
            "VALUES (#{fromId}, #{toId}, #{message}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FriendRequestEntity entity);
    
    @Update("UPDATE friend_request SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") int status, @Param("updateTime") long updateTime);
}
