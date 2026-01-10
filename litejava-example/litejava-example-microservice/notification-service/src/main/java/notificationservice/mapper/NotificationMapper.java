package notificationservice.mapper;

import notificationservice.model.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface NotificationMapper {
    
    @Select("SELECT * FROM notifications WHERE id = #{id}")
    Notification findById(Long id);
    
    @Select("SELECT * FROM notifications WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Notification> findByUserId(Long userId);
    
    @Select("SELECT * FROM notifications WHERE status = #{status} ORDER BY created_at LIMIT #{limit}")
    List<Notification> findByStatus(@Param("status") Integer status, @Param("limit") Integer limit);
    
    @Insert("INSERT INTO notifications (user_id, type, title, content, target, status) " +
            "VALUES (#{userId}, #{type}, #{title}, #{content}, #{target}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Notification notification);
    
    @Update("UPDATE notifications SET status = #{status}, error_msg = #{errorMsg}, sent_at = NOW() WHERE id = #{id}")
    int updateStatus(Notification notification);
}
