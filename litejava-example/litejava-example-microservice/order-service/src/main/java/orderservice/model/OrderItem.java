package orderservice.model;

import java.math.BigDecimal;

/**
 * 订单项实体
 * 
 * <p>对应数据库 order_items 表，表示订单中的单个商品
 * 
 * @author LiteJava
 */
public class OrderItem {
    
    /** 订单项 ID */
    public Long id;
    
    /** 所属订单 ID */
    public Long orderId;
    
    /** 商品名称 */
    public String productName;
    
    /** 商品单价 */
    public BigDecimal price;
    
    /** 购买数量 */
    public Integer quantity;
}
