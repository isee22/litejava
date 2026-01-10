package orderservice.mapper;

import orderservice.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单数据访问层
 * 
 * @author LiteJava
 */
public interface OrderMapper {
    
    /**
     * 根据 ID 查询订单
     */
    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(Long id);
    
    /**
     * 查询所有订单
     */
    @Select("SELECT * FROM orders ORDER BY created_at DESC")
    List<Order> findAll();
    
    /**
     * 根据用户 ID 查询订单
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Order> findByUserId(Long userId);
    
    /**
     * 插入订单
     */
    @Insert("INSERT INTO orders (order_no, user_id, total_amount, status) VALUES (#{orderNo}, #{userId}, #{totalAmount}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);
    
    /**
     * 更新订单状态
     */
    @Update("UPDATE orders SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 根据订单号更新状态
     */
    @Update("UPDATE orders SET status = #{status}, updated_at = NOW() WHERE order_no = #{orderNo}")
    int updateStatusByOrderNo(@Param("orderNo") String orderNo, @Param("status") Integer status);
}
