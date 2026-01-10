package userservice;

import litejava.App;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import userservice.mapper.UserMapper;

/**
 * 全局业务组件
 */
public class G {
    
    public static App app;
    
    public static UserMapper userMapper;
    
    public static void init() {
        userMapper = app.getPlugin(MyBatisPlugin.class).getMapper(UserMapper.class);
    }
    
    public static ConsulPlugin consul() {
        return app.getPlugin(ConsulPlugin.class);
    }
}
