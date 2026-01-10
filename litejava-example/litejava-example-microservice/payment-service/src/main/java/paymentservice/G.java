package paymentservice;

import litejava.App;
import litejava.plugins.cache.RedisCachePlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.microservice.CircuitBreaker;
import litejava.plugins.microservice.ConsulPlugin;
import paymentservice.mapper.PaymentMapper;
import paymentservice.mapper.RefundMapper;

/**
 * 全局业务组件
 * 
 * @author LiteJava
 */
public class G {
    
    public static App app;
    
    // 业务组件
    public static PaymentMapper paymentMapper;
    public static RefundMapper refundMapper;
    public static CircuitBreaker circuitBreaker;
    
    public static void init() {
        MyBatisPlugin mybatis = app.getPlugin(MyBatisPlugin.class);
        paymentMapper = mybatis.getMapper(PaymentMapper.class);
        refundMapper = mybatis.getMapper(RefundMapper.class);
        circuitBreaker = new CircuitBreaker();
    }
    
    // 插件快捷访问
    public static RedisCachePlugin redis() {
        return app.getPlugin(RedisCachePlugin.class);
    }
    
    public static ConsulPlugin consul() {
        return app.getPlugin(ConsulPlugin.class);
    }
}
