package orderservice;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.cache.RedisCachePlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.http.RpcClient;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import litejava.plugins.microservice.DiscoveryPlugin.ServiceInstance;
import litejava.plugins.mq.MqPlugin;
import litejava.plugins.tracing.TracingPlugin;
import litejava.plugins.transaction.SeataPlugin;
import litejava.plugins.transaction.SeataFilter;
import orderservice.controller.OrderController;
import orderservice.mapper.OrderItemMapper;
import orderservice.mapper.OrderMapper;

import java.util.List;

/**
 * 订单服务启动类
 */
public class OrderServiceApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        // 统一异常处理（必须在路由之前注册）
        app.use(new RecoveryPlugin());
        
        // 基础设施插件
        app.use(new MyBatisPlugin(OrderMapper.class, OrderItemMapper.class));
        app.use(new RedisCachePlugin());  // Redis 缓存
        app.use(new RpcClient());         // RPC 客户端
        app.use(new MqPlugin());          // 消息队列
        app.use(new ConsulPlugin());
        app.use(new TracingPlugin());     // 链路追踪
        app.use(new SeataPlugin());       // 分布式事务
        app.use(new SeataFilter());       // 自动传递 XID
        
        HealthCheck health = new HealthCheck();
        health.addCheck("db", () -> {
            try {
                G.orderMapper.findAll();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        health.addReadinessCheck("user-service", () -> {
            List<ServiceInstance> instances = G.consul().getInstances("user-service");
            return instances != null && !instances.isEmpty();
        });
        app.use(health);
        
        G.init();
        
        // 订阅支付成功消息
        MqPlugin mq = app.getPlugin(MqPlugin.class);
        if (mq != null && mq.isConnected()) {
            mq.subscribeJson("payment.success", data -> {
                String orderNo = (String) data.get("orderNo");
                app.log.info("[MQ] 收到支付成功通知: " + orderNo);
                // 更新订单状态为已支付
                G.orderMapper.updateStatusByOrderNo(orderNo, 2);
            });
        } else {
            app.log.warn("[MQ] 消息队列未连接，跳过订阅");
        }
        
        app.register(OrderController.routes());
        app.run(app.conf.getInt("server", "port", 8082));
    }
}
