package notificationservice;

import litejava.App;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.microservice.ConsulPlugin;
import notificationservice.mapper.NotificationMapper;
import notificationservice.mapper.TemplateMapper;

/**
 * 全局业务组件
 */
public class G {
    
    public static App app;
    
    public static NotificationMapper notificationMapper;
    public static TemplateMapper templateMapper;
    
    public static void init() {
        MyBatisPlugin mybatis = app.getPlugin(MyBatisPlugin.class);
        notificationMapper = mybatis.getMapper(NotificationMapper.class);
        templateMapper = mybatis.getMapper(TemplateMapper.class);
    }
    
    public static ConsulPlugin consul() {
        return app.getPlugin(ConsulPlugin.class);
    }
}
