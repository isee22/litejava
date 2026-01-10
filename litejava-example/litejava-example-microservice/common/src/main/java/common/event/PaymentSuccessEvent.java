package common.event;

import java.math.BigDecimal;

/**
 * 支付成功事件
 */
public class PaymentSuccessEvent {
    public String orderNo;
    public String paymentNo;
    public Long userId;
    public BigDecimal amount;
    
    public PaymentSuccessEvent() {}
    
    public PaymentSuccessEvent(String orderNo, String paymentNo, Long userId, BigDecimal amount) {
        this.orderNo = orderNo;
        this.paymentNo = paymentNo;
        this.userId = userId;
        this.amount = amount;
    }
}
