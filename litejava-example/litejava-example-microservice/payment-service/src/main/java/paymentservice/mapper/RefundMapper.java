package paymentservice.mapper;

import org.apache.ibatis.annotations.*;
import paymentservice.model.Refund;

import java.util.List;

public interface RefundMapper {
    
    @Select("SELECT * FROM refunds WHERE refund_no = #{refundNo}")
    Refund findByRefundNo(String refundNo);
    
    @Select("SELECT * FROM refunds WHERE payment_no = #{paymentNo}")
    List<Refund> findByPaymentNo(String paymentNo);
    
    @Insert("INSERT INTO refunds (refund_no, payment_no, amount, reason, status) " +
            "VALUES (#{refundNo}, #{paymentNo}, #{amount}, #{reason}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Refund refund);
    
    @Update("UPDATE refunds SET status = #{status}, updated_at = NOW() WHERE refund_no = #{refundNo}")
    int updateStatus(@Param("refundNo") String refundNo, @Param("status") Integer status);
}
