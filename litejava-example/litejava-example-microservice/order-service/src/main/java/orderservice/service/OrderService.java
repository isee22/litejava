package orderservice.service;

import common.BizException;
import common.Err;
import litejava.plugins.transaction.SeataPlugin;
import orderservice.G;
import orderservice.model.Order;
import orderservice.model.OrderItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务
 * 
 * @author LiteJava
 */
public class OrderService {
    
    public static List<Order> findAll() {
        return G.orderMapper.findAll();
    }
    
    public static Order findById(Long id) {
        Order order = G.orderMapper.findById(id);
        if (order == null) {
            return null;
        }
        
        order.items = G.orderItemMapper.findByOrderId(id);
        order.user = UserClient.getUser(order.userId);
        
        return order;
    }
    
    public static List<Order> findByUserId(Long userId) {
        return G.orderMapper.findByUserId(userId);
    }
    
    /**
     * 创建订单（分布式锁防重复）
     */
    public static Order create(Long userId, List<OrderItem> items) {
        String lockKey = "lock:order:create:" + userId;
        
        return G.redis().withLock(lockKey, 30, () -> {
            if (!UserClient.userExists(userId)) BizException.error(Err.USER_NOT_FOUND, "用户不存在或用户服务不可用");
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderItem item : items) {
                BigDecimal itemTotal = item.price.multiply(BigDecimal.valueOf(item.quantity));
                totalAmount = totalAmount.add(itemTotal);
            }
            
            Order order = new Order();
            order.orderNo = "ORD" + System.currentTimeMillis();
            order.userId = userId;
            order.totalAmount = totalAmount;
            order.status = Order.STATUS_PENDING;
            
            G.orderMapper.insert(order);
            
            for (OrderItem item : items) {
                item.orderId = order.id;
                G.orderItemMapper.insert(item);
            }
            
            order.items = items;
            return order;
        });
    }
    
    /**
     * 创建订单（分布式事务版本）
     * 包含：创建订单 + 扣减库存 + 扣减余额
     */
    public static Order createWithTransaction(Long userId, List<OrderItem> items) {
        SeataPlugin seata = G.app.getPlugin(SeataPlugin.class);
        
        return seata.execute(() -> {
            // 1. 验证用户
            if (!UserClient.userExists(userId)) BizException.error(Err.USER_NOT_FOUND, "用户不存在");
            
            // 2. 计算总金额
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderItem item : items) {
                BigDecimal itemTotal = item.price.multiply(BigDecimal.valueOf(item.quantity));
                totalAmount = totalAmount.add(itemTotal);
            }
            
            // 3. 创建订单
            Order order = new Order();
            order.orderNo = "ORD" + System.currentTimeMillis();
            order.userId = userId;
            order.totalAmount = totalAmount;
            order.status = Order.STATUS_PENDING;
            G.orderMapper.insert(order);
            
            for (OrderItem item : items) {
                item.orderId = order.id;
                G.orderItemMapper.insert(item);
            }
            
            // 4. 扣减库存（调用 product-service）
            // ProductClient.deductStock(productId, quantity);
            
            // 5. 扣减余额（调用 user-service）
            // UserClient.deductBalance(userId, totalAmount);
            
            order.items = items;
            return order;
        });
        // 任一步骤失败，全部自动回滚
    }
    
    public static boolean updateStatus(Long id, Integer status) {
        return G.orderMapper.updateStatus(id, status) > 0;
    }
    
    public static boolean updateStatusByOrderNo(String orderNo, Integer status) {
        return G.orderMapper.updateStatusByOrderNo(orderNo, status) > 0;
    }
}
