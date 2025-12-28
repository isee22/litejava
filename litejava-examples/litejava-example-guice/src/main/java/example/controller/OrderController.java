package example.controller;

import example.service.OrderService;
import example.service.NotificationService;
import litejava.Context;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

/**
 * 订单控制器 - 演示字段注入 + @Named
 * 
 * <p>字段注入的优缺点：
 * <ul>
 *   <li>优点：代码简洁，无需构造函数</li>
 *   <li>缺点：依赖不明显，不便于测试，字段不能是 final</li>
 * </ul>
 * 
 * <p>一般推荐构造函数注入，但字段注入在某些场景也可接受。
 */
public class OrderController {
    
    /**
     * 字段注入（简洁但不推荐）
     */
    @Inject
    OrderService orderService;
    
    /**
     * 字段注入 + @Named 选择特定实现
     */
    @Inject
    @Named("sms")
    NotificationService smsService;
    
    public void list(Context ctx) {
        ctx.json(Map.of("list", orderService.list()));
    }
    
    public void get(Context ctx) {
        long id = ctx.pathParamLong("id");
        Map<String, Object> order = orderService.get(id);
        if (order == null) {
            ctx.status(404).json(Map.of("error", "Order not found"));
            return;
        }
        ctx.json(order);
    }
    
    public void create(Context ctx) {
        Map<String, Object> data = ctx.bindJSON();
        Map<String, Object> order = orderService.create(data);
        
        // 额外发送短信通知
        smsService.send("13800138000", "New order #" + order.get("id"));
        
        ctx.status(201).json(order);
    }
}
