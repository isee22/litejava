package orderservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体
 * 
 * <p>对应数据库 orders 表
 * 
 * <h2>状态说明</h2>
 * <ul>
 *   <li>0 - 待支付：订单已创建，等待用户支付</li>
 *   <li>1 - 已支付：用户已完成支付</li>
 *   <li>2 - 已发货：商家已发货</li>
 *   <li>3 - 已完成：用户确认收货</li>
 *   <li>4 - 已取消：订单已取消</li>
 * </ul>
 * 
 * @author LiteJava
 */
public class Order {
    
    // ========== 状态常量 ==========
    
    /** 待支付 */
    public static final int STATUS_PENDING = 0;
    
    /** 已支付 */
    public static final int STATUS_PAID = 1;
    
    /** 已发货 */
    public static final int STATUS_SHIPPED = 2;
    
    /** 已完成 */
    public static final int STATUS_COMPLETED = 3;
    
    /** 已取消 */
    public static final int STATUS_CANCELLED = 4;
    
    // ========== 字段 ==========
    
    /** 订单 ID */
    public Long id;
    
    /** 订单号（唯一） */
    public String orderNo;
    
    /** 用户 ID */
    public Long userId;
    
    /** 订单总金额 */
    public BigDecimal totalAmount;
    
    /** 订单状态 */
    public Integer status;
    
    /** 创建时间 */
    public LocalDateTime createdAt;
    
    /** 更新时间 */
    public LocalDateTime updatedAt;
    
    // ========== 关联数据 ==========
    
    /** 订单项列表 */
    public List<OrderItem> items;
    
    /** 用户信息（来自 user-service） */
    public Object user;
}
