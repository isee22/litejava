package game.account.mapper;

import game.account.entity.AccountEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountMapper {
    
    @Select("SELECT id, username, password, create_time as createTime, last_login_time as lastLoginTime " +
            "FROM account WHERE username = #{username}")
    AccountEntity findByUsername(String username);
    
    @Select("SELECT id, username, password, create_time as createTime, last_login_time as lastLoginTime " +
            "FROM account WHERE id = #{id}")
    AccountEntity findById(long id);
    
    @Insert("INSERT INTO account (username, password, create_time, last_login_time) " +
            "VALUES (#{username}, #{password}, #{createTime}, #{lastLoginTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AccountEntity entity);
    
    @Update("UPDATE account SET last_login_time = #{lastLoginTime} WHERE id = #{id}")
    int updateLoginTime(@Param("id") long id, @Param("lastLoginTime") long lastLoginTime);
}
