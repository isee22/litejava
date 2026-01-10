package authservice;

import litejava.App;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import authservice.mapper.AccountMapper;
import authservice.service.JwtService;

/**
 * 全局业务组件
 */
public class G {
    
    public static App app;
    
    public static AccountMapper accountMapper;
    public static JwtService jwt = new JwtService();
    
    public static void init() {
        accountMapper = app.getPlugin(MyBatisPlugin.class).getMapper(AccountMapper.class);
        jwt.init();
    }
    
    public static ConsulPlugin consul() {
        return app.getPlugin(ConsulPlugin.class);
    }
}
