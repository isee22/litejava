package game.account.mapper;

import game.account.entity.Account;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountMapper {
    
    @Select("SELECT * FROM account WHERE username = #{username}")
    Account findByUsername(String username);
    
    @Select("SELECT * FROM account WHERE id = #{id}")
    Account findById(long id);
    
    @Insert("INSERT INTO account (username, password, createTime, lastLoginTime) " +
            "VALUES (#{username}, #{password}, #{createTime}, #{lastLoginTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Account entity);
    
    @Update("UPDATE account SET lastLoginTime = #{lastLoginTime} WHERE id = #{id}")
    int updateLoginTime(@Param("id") long id, @Param("lastLoginTime") long lastLoginTime);
}
