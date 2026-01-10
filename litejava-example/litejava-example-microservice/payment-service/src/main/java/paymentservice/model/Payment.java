package paymentservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    public Long id;
    public String paymentNo;
    public String orderNo;
    public Long userId;
    public BigDecimal amount;
    public String channel; // alipay/wechat/bank
    public Integer status; // 0:待支付 1:支付中 2:成功 3:失败 4:已退款
    public LocalDateTime paidAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_REFUNDED = 4;
}
