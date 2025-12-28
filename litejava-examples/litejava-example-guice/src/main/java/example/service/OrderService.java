package example.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务 - 演示类级别 @Singleton 注解
 * 
 * <p>类上标注 @Singleton 后，Guice 绑定时无需再指定作用域：
 * {@code binder.bind(OrderService.class)} 即可
 */
@Singleton
public class OrderService {
    
    private final UserService userService;
    private final NotificationService emailService;
    private final Map<Long, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(0);
    
    /**
     * 构造函数注入 + @Named 选择特定实现
     */
    @Inject
    public OrderService(UserService userService, 
                        @Named("email") NotificationService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }
    
    public List<Map<String, Object>> list() {
        return new ArrayList<>(orders.values());
    }
    
    public Map<String, Object> get(long id) {
        Map<String, Object> order = orders.get(id);
        if (order == null) return null;
        
        // 关联用户信息
        Map<String, Object> result = new HashMap<>(order);
        long userid = ((Number) order.get("userid")).longValue();
        result.put("user", userService.get(userid));
        return result;
    }
    
    public Map<String, Object> create(Map<String, Object> data) {
        long id = idGen.incrementAndGet();
        Map<String, Object> order = new HashMap<>(data);
        order.put("id", id);
        order.put("status", "pending");
        order.put("createdat", System.currentTimeMillis());
        orders.put(id, order);
        
        // 发送邮件通知
        long userid = ((Number) data.get("userid")).longValue();
        Map<String, Object> user = userService.get(userid);
        if (user != null) {
            String email = (String) user.get("email");
            if (email != null) {
                emailService.send(email, "Your order #" + id + " has been created!");
            }
        }
        
        return order;
    }
}
