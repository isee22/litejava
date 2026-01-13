package game.account.mapper;

import game.account.entity.ItemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ItemConfigMapper {
    
    @Select("SELECT * FROM item_config WHERE enabled = 1 ORDER BY sortOrder")
    List<ItemConfig> findAllEnabled();
    
    @Select("SELECT * FROM item_config WHERE type = #{type} AND enabled = 1 ORDER BY sortOrder")
    List<ItemConfig> findByType(String type);
    
    @Select("SELECT * FROM item_config WHERE itemId = #{itemId}")
    ItemConfig findById(int itemId);
}
