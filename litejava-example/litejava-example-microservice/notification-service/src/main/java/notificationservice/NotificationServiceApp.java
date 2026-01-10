package notificationservice;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import notificationservice.controller.NotificationController;
import notificationservice.mapper.NotificationMapper;
import notificationservice.mapper.TemplateMapper;

/**
 * 通知服务启动类
 */
public class NotificationServiceApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        app.use(new RecoveryPlugin());  // 统一异常处理
        app.use(new MyBatisPlugin(NotificationMapper.class, TemplateMapper.class));
        app.use(new ConsulPlugin());
        
        HealthCheck health = new HealthCheck();
        health.addCheck("db", () -> {
            try {
                G.templateMapper.findAll();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        app.use(health);
        
        G.init();
        app.register(NotificationController.routes());
        app.run(app.conf.getInt("server", "port", 8086));
    }
}
