package example.mapper;

import example.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户 Mapper - MyBatis 注解方式
 */
@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users ORDER BY id")
    List<User> findAll();
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);
    
    @Insert("INSERT INTO users (name, email) VALUES (#{name}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE users SET name = #{name}, email = #{email} WHERE id = #{id}")
    int update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(Long id);
}
