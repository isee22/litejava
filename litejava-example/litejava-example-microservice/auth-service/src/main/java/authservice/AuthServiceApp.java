package authservice;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import litejava.plugins.microservice.HealthCheck;
import authservice.controller.AuthController;
import authservice.mapper.AccountMapper;

/**
 * 认证服务启动类
 */
public class AuthServiceApp {
    
    public static void main(String[] args) {
        App app = G.app = LiteJava.create();
        
        app.use(new RecoveryPlugin());  // 统一异常处理
        app.use(new MyBatisPlugin(AccountMapper.class));
        app.use(new ConsulPlugin());
        
        HealthCheck health = new HealthCheck();
        health.addCheck("db", () -> {
            try {
                G.accountMapper.findByUsername("_health_check_");
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        app.use(health);
        
        G.init();
        app.register(AuthController.routes());
        app.run(app.conf.getInt("server", "port", 8085));
    }
}
