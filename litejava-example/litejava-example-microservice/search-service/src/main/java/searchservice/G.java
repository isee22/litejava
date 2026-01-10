package searchservice;

import litejava.App;
import litejava.plugins.microservice.CircuitBreaker;
import litejava.plugins.microservice.ConsulPlugin;

/**
 * 全局业务组件
 */
public class G {
    
    public static App app;
    
    public static CircuitBreaker circuitBreaker;
    
    public static void init() {
        circuitBreaker = new CircuitBreaker();
    }
    
    public static ConsulPlugin consul() {
        return app.getPlugin(ConsulPlugin.class);
    }
}
