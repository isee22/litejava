package paymentservice;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.cache.RedisCachePlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.http.RpcClient;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import litejava.plugins.mq.MqPlugin;
import paymentservice.controller.PaymentController;
import paymentservice.mapper.PaymentMapper;
import paymentservice.mapper.RefundMapper;

/**
 * 支付服务启动类
 */
public class PaymentServiceApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        // 统一异常处理（必须在路由之前注册）
        app.use(new RecoveryPlugin());
        
        app.use(new MyBatisPlugin(PaymentMapper.class, RefundMapper.class));
        app.use(new RedisCachePlugin());
        
        // 服务发现（必须在 RpcClient 之前注册）
        ConsulPlugin consul = new ConsulPlugin();
        app.use(consul);
        
        // RPC 客户端（配置服务发现）
        RpcClient rpc = new RpcClient();
        rpc.discovery(consul);
        app.use(rpc);
        
        app.use(new MqPlugin());      // 消息队列
        
        HealthCheck health = new HealthCheck();
        health.addCheck("db", () -> {
            try {
                G.paymentMapper.findAll();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        app.use(health);
        
        G.init();
        app.register(PaymentController.routes());
        app.run(app.conf.getInt("server", "port", 8084));
    }
}
