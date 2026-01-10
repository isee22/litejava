package paymentservice.mapper;

import org.apache.ibatis.annotations.*;
import paymentservice.model.Payment;

import java.util.List;

public interface PaymentMapper {
    
    @Select("SELECT * FROM payments WHERE id = #{id}")
    Payment findById(Long id);
    
    @Select("SELECT * FROM payments WHERE payment_no = #{paymentNo}")
    Payment findByPaymentNo(String paymentNo);
    
    @Select("SELECT * FROM payments WHERE order_no = #{orderNo}")
    Payment findByOrderNo(String orderNo);
    
    @Select("SELECT * FROM payments WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Payment> findByUserId(Long userId);
    
    @Select("SELECT * FROM payments ORDER BY created_at DESC")
    List<Payment> findAll();
    
    @Insert("INSERT INTO payments (payment_no, order_no, user_id, amount, channel, status) " +
            "VALUES (#{paymentNo}, #{orderNo}, #{userId}, #{amount}, #{channel}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Payment payment);
    
    @Update("UPDATE payments SET status = #{status}, paid_at = #{paidAt}, updated_at = NOW() WHERE payment_no = #{paymentNo}")
    int updateStatus(Payment payment);
}
