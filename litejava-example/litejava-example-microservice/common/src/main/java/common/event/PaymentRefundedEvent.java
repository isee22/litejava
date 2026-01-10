package common.event;

import java.math.BigDecimal;

/**
 * 退款成功事件
 */
public class PaymentRefundedEvent {
    public String orderNo;
    public String paymentNo;
    public String refundNo;
    public BigDecimal amount;
    
    public PaymentRefundedEvent() {}
    
    public PaymentRefundedEvent(String orderNo, String paymentNo, String refundNo, BigDecimal amount) {
        this.orderNo = orderNo;
        this.paymentNo = paymentNo;
        this.refundNo = refundNo;
        this.amount = amount;
    }
}
