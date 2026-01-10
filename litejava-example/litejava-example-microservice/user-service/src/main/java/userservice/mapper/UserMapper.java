package userservice.mapper;

import org.apache.ibatis.annotations.*;
import userservice.model.User;

import java.util.List;

/**
 * 用户数据访问层
 * 
 * <p>使用 MyBatis 注解方式定义 SQL，字段映射自动处理（下划线转驼峰）
 * 
 * <h2>表结构</h2>
 * <pre>
 * CREATE TABLE users (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     username VARCHAR(50) NOT NULL UNIQUE,
 *     email VARCHAR(100) NOT NULL,
 *     phone VARCHAR(20),
 *     status TINYINT DEFAULT 1,  -- 1:正常 0:禁用
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 * );
 * </pre>
 * 
 * @author LiteJava
 */
public interface UserMapper {
    
    /**
     * 根据 ID 查询用户
     * 
     * @param id 用户 ID
     * @return 用户信息，只返回状态正常的用户
     */
    @Select("SELECT * FROM users WHERE id = #{id} AND status = 1")
    User findById(Long id);
    
    /**
     * 查询所有用户
     * 
     * @return 用户列表，按创建时间倒序
     */
    @Select("SELECT * FROM users WHERE status = 1 ORDER BY created_at DESC")
    List<User> findAll();
    
    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND status = 1")
    User findByUsername(String username);
    
    /**
     * 插入用户
     * 
     * <p>使用 useGeneratedKeys 自动回填主键
     * 
     * @param user 用户信息
     */
    @Insert("INSERT INTO users (username, email, phone, status) VALUES (#{username}, #{email}, #{phone}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    /**
     * 更新用户信息
     * 
     * <p>只更新 email 和 phone 字段
     * 
     * @param user 用户信息（需包含 id）
     * @return 影响行数
     */
    @Update("UPDATE users SET email = #{email}, phone = #{phone}, updated_at = NOW() WHERE id = #{id}")
    int update(User user);
    
    /**
     * 删除用户（软删除）
     * 
     * <p>将 status 设置为 0
     * 
     * @param id 用户 ID
     * @return 影响行数
     */
    @Update("UPDATE users SET status = 0, updated_at = NOW() WHERE id = #{id}")
    int delete(Long id);
}
