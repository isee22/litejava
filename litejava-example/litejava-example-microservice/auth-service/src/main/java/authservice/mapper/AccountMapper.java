package authservice.mapper;

import authservice.model.Account;
import org.apache.ibatis.annotations.*;

public interface AccountMapper {
    
    @Select("SELECT * FROM accounts WHERE username = #{username} AND status = 1")
    Account findByUsername(String username);
    
    @Select("SELECT * FROM accounts WHERE user_id = #{userId} AND status = 1")
    Account findByUserId(Long userId);
    
    @Insert("INSERT INTO accounts (user_id, username, password_hash, status) VALUES (#{userId}, #{username}, #{passwordHash}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Account account);
    
    @Update("UPDATE accounts SET password_hash = #{passwordHash}, updated_at = NOW() WHERE user_id = #{userId}")
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
    
    @Update("UPDATE accounts SET last_login_at = NOW() WHERE user_id = #{userId}")
    int updateLastLogin(Long userId);
    
    @Update("UPDATE accounts SET status = 0 WHERE user_id = #{userId}")
    int disable(Long userId);
}
