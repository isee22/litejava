package orderservice.mapper;

import orderservice.model.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单项数据访问层
 * 
 * @author LiteJava
 */
public interface OrderItemMapper {
    
    /**
     * 根据订单 ID 查询订单项
     */
    @Select("SELECT * FROM order_items WHERE order_id = #{orderId}")
    List<OrderItem> findByOrderId(Long orderId);
    
    /**
     * 插入订单项
     */
    @Insert("INSERT INTO order_items (order_id, product_name, price, quantity) VALUES (#{orderId}, #{productName}, #{price}, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrderItem item);
    
    /**
     * 删除订单的所有订单项
     */
    @Delete("DELETE FROM order_items WHERE order_id = #{orderId}")
    int deleteByOrderId(Long orderId);
}
