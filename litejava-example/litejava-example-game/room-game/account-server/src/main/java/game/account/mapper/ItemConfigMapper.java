package game.account.mapper;

import game.account.entity.ItemConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ItemConfigMapper {
    
    @Select("SELECT item_id as itemId, name, type, `desc`, price, coin_price as coinPrice, " +
            "duration, effect, enabled, sort_order as sortOrder " +
            "FROM item_config WHERE enabled = 1 ORDER BY sort_order")
    List<ItemConfigEntity> findAllEnabled();
    
    @Select("SELECT item_id as itemId, name, type, `desc`, price, coin_price as coinPrice, " +
            "duration, effect, enabled, sort_order as sortOrder " +
            "FROM item_config WHERE type = #{type} AND enabled = 1 ORDER BY sort_order")
    List<ItemConfigEntity> findByType(String type);
    
    @Select("SELECT item_id as itemId, name, type, `desc`, price, coin_price as coinPrice, " +
            "duration, effect, enabled, sort_order as sortOrder " +
            "FROM item_config WHERE item_id = #{itemId}")
    ItemConfigEntity findById(int itemId);
}
