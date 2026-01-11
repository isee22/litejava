package game.account.mapper;

import game.account.entity.SignInConfigEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SignInConfigMapper {
    
    @Select("SELECT * FROM sign_in_config ORDER BY day")
    List<SignInConfigEntity> findAll();
    
    @Select("SELECT * FROM sign_in_config WHERE day = #{day}")
    SignInConfigEntity findByDay(@Param("day") int day);
}
