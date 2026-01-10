package paymentservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Refund {
    public Long id;
    public String refundNo;
    public String paymentNo;
    public BigDecimal amount;
    public String reason;
    public Integer status; // 0:待处理 1:处理中 2:成功 3:失败
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
