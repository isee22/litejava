package productservice;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import productservice.controller.ProductController;
import productservice.mapper.CategoryMapper;
import productservice.mapper.ProductMapper;

/**
 * 商品服务启动类
 */
public class ProductServiceApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        app.use(new RecoveryPlugin());  // 统一异常处理
        app.use(new MyBatisPlugin(ProductMapper.class, CategoryMapper.class));
        app.use(new ConsulPlugin());
        
        HealthCheck health = new HealthCheck();
        health.addCheck("db", () -> {
            try {
                G.productMapper.findAll();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        app.use(health);
        
        G.init();
        app.register(ProductController.routes());
        app.run(app.conf.getInt("server", "port", 8083));
    }
}
