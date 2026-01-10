package gateway;

import litejava.App;
import litejava.plugins.microservice.CircuitBreaker;

/**
 * 全局业务组件 (K8s 版)
 */
public class G {
    
    public static App app;
    
    public static CircuitBreaker circuitBreaker;
    
    public static void init() {
        circuitBreaker = new CircuitBreaker();
    }
}
