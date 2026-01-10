package userservice;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import userservice.controller.UserController;
import userservice.mapper.UserMapper;

/**
 * 用户服务启动类
 */
public class UserServiceApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        app.use(new RecoveryPlugin());  // 统一异常处理
        app.use(new MyBatisPlugin(UserMapper.class));
        app.use(new ConsulPlugin());
        
        HealthCheck health = new HealthCheck();
        health.addCheck("db", () -> {
            try {
                G.userMapper.findAll();
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        app.use(health);
        
        G.init();
        app.register(UserController.routes());
        
        int port = app.conf.getInt("server", "port", 8081);
        app.log.info("User Service starting on port " + port);
        app.run(port);
    }
}
