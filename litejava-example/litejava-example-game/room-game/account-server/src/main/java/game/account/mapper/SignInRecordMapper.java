package game.account.mapper;

import game.account.entity.SignInRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SignInRecordMapper {
    
    @Select("SELECT * FROM sign_in_record WHERE userId = #{userId}")
    SignInRecord findByUserId(@Param("userId") long userId);
    
    @Insert("INSERT INTO sign_in_record (userId, signDay, lastSignTime) VALUES (#{userId}, #{signDay}, #{lastSignTime})")
    int insert(SignInRecord entity);
    
    @Update("UPDATE sign_in_record SET signDay = #{signDay}, lastSignTime = #{lastSignTime} WHERE userId = #{userId}")
    int update(SignInRecord entity);
}
