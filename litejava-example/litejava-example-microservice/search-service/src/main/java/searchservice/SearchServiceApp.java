package searchservice;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import searchservice.controller.SearchController;

/**
 * 搜索服务启动类
 */
public class SearchServiceApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        app.use(new RecoveryPlugin());  // 统一异常处理
        app.use(new ConsulPlugin());
        
        HealthCheck health = new HealthCheck();
        health.addCheck("self", () -> true);
        app.use(health);
        
        G.init();
        app.register(SearchController.routes());
        app.run(app.conf.getInt("server", "port", 8087));
    }
}
