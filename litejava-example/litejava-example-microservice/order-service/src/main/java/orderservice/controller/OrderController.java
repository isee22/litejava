package orderservice.controller;

import common.BizException;
import common.Err;
import common.vo.ListResult;
import litejava.Context;
import litejava.Routes;
import orderservice.model.Order;
import orderservice.model.OrderItem;
import orderservice.service.OrderService;
import orderservice.service.UserClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
public class OrderController {
    
    public static Routes routes() {
        return new Routes()
            .post("/order/list", OrderController::list)
            .post("/order/detail", OrderController::detail)
            .post("/order/create", OrderController::create)
            .post("/order/updateStatus", OrderController::updateStatus)
            .post("/order/listByUser", OrderController::listByUser)
            .post("/order/circuitBreaker", OrderController::circuitBreakerStatus)
            .end();
    }
    
    static void list(Context ctx) {
        String userIdStr = ctx.header("X-User-Id");
        
        List<Order> orders;
        if (userIdStr != null && !userIdStr.isEmpty()) {
            Long userId = Long.parseLong(userIdStr);
            orders = OrderService.findByUserId(userId);
        } else {
            orders = new ArrayList<>();
        }
        
        ctx.ok(ListResult.of(orders));
    }
    
    static void detail(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("id") == null) BizException.paramRequired("id");
        
        Long id = ((Number) body.get("id")).longValue();
        Order order = OrderService.findById(id);
        if (order == null) BizException.error(Err.ORDER_NOT_FOUND, "订单不存在");
        
        ctx.ok(order);
    }
    
    @SuppressWarnings("unchecked")
    static void create(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        
        // 优先从 header 获取 userId（Gateway 传递），其次从 body 获取
        String userIdStr = ctx.header("X-User-Id");
        Long userId;
        if (userIdStr != null && !userIdStr.isEmpty()) {
            userId = Long.parseLong(userIdStr);
        } else if (body.get("userId") != null) {
            userId = ((Number) body.get("userId")).longValue();
        } else {
            ctx.fail(401, "未登录");
            return;
        }
        
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) body.get("items");
        if (itemsData == null || itemsData.isEmpty()) BizException.error(Err.ORDER_ITEM_EMPTY, "订单项不能为空");
        
        List<OrderItem> items = new ArrayList<>();
        for (Map<String, Object> itemData : itemsData) {
            OrderItem item = new OrderItem();
            item.productName = (String) itemData.get("productName");
            item.price = new BigDecimal(itemData.get("price").toString());
            item.quantity = ((Number) itemData.get("quantity")).intValue();
            items.add(item);
        }
        
        // 业务异常由 RecoveryPlugin 统一处理
        Order order = OrderService.create(userId, items);
        ctx.ok(order);
    }
    
    static void updateStatus(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("status") == null) BizException.paramRequired("status");
        
        Integer status = ((Number) body.get("status")).intValue();
        boolean success = false;
        
        if (body.get("id") != null) {
            Long id = ((Number) body.get("id")).longValue();
            success = OrderService.updateStatus(id, status);
        } else if (body.get("orderNo") != null) {
            String orderNo = (String) body.get("orderNo");
            success = OrderService.updateStatusByOrderNo(orderNo, status);
        } else {
            BizException.paramRequired("id 或 orderNo");
        }
        
        if (!success) BizException.error(Err.ORDER_NOT_FOUND, "订单不存在");
        ctx.ok();
    }
    
    static void listByUser(Context ctx) {
        Map<String, Object> body = ctx.bindJSON();
        if (body.get("userId") == null) BizException.paramRequired("userId");
        
        Long userId = ((Number) body.get("userId")).longValue();
        List<Order> orders = OrderService.findByUserId(userId);
        ctx.ok(ListResult.of(orders));
    }
    
    static void circuitBreakerStatus(Context ctx) {
        String state = UserClient.getCircuitBreakerState();
        ctx.ok(Map.of("user-service", state));
    }
}
