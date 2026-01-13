package game.account.mapper;

import game.account.entity.SignInConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SignInConfigMapper {
    
    @Select("SELECT * FROM sign_in_config ORDER BY day")
    List<SignInConfig> findAll();
    
    @Select("SELECT * FROM sign_in_config WHERE day = #{day}")
    SignInConfig findByDay(@Param("day") int day);
}
