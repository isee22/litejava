package orderservice;

import litejava.App;
import litejava.plugins.cache.RedisCachePlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.microservice.CircuitBreaker;
import litejava.plugins.microservice.ConsulPlugin;
import orderservice.mapper.OrderItemMapper;
import orderservice.mapper.OrderMapper;

/**
 * 全局业务组件
 */
public class G {
    
    public static App app;
    
    public static OrderMapper orderMapper;
    public static OrderItemMapper orderItemMapper;
    public static CircuitBreaker circuitBreaker;
    
    public static void init() {
        MyBatisPlugin mybatis = app.getPlugin(MyBatisPlugin.class);
        orderMapper = mybatis.getMapper(OrderMapper.class);
        orderItemMapper = mybatis.getMapper(OrderItemMapper.class);
        circuitBreaker = new CircuitBreaker();
    }
    
    public static RedisCachePlugin redis() {
        return app.getPlugin(RedisCachePlugin.class);
    }
    
    public static ConsulPlugin consul() {
        return app.getPlugin(ConsulPlugin.class);
    }
}
