package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.annotation.SpringMvcAnnotationPlugin;
import litejava.plugins.cache.MemoryCachePlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.dataSource.HikariPlugin;
import litejava.plugins.di.GuicePlugin;
import litejava.plugins.schedule.SchedulePlugin;
import litejava.plugins.view.ThymeleafPlugin;

/**
 * LiteJava Spring Boot 风格示例 - 纯配置方式
 * 
 * 插件配置从 application-conf.yml 读取，但加载顺序由代码控制
 */
public class SpringBootConfApp {
    
    public static void main(String[] args) {
        // 使用专用配置文件
        App app = LiteJava.create("application-conf.yml");
        
        // 中间件
        app.use(new ThymeleafPlugin());
        
        // 数据源 + MyBatis
        HikariPlugin hikari = new HikariPlugin();
        MyBatisPlugin mybatis = new MyBatisPlugin(hikari);
        app.use(hikari);
        app.use(mybatis);
        
        // 缓存
        app.use(new MemoryCachePlugin());
        
        // DI（绑定 Mapper）- 必须在 SpringMvcAnnotationPlugin 之前
        GuicePlugin di = new GuicePlugin();
        di.bind(binder -> {
            for (Class<?> mapperClass : mybatis.getMapperClasses()) {
                binder.bind((Class<Object>) mapperClass).toInstance(mybatis.getMapper(mapperClass));
            }
        });
        app.use(di);
        
        // 定时任务（注解方式）
        SchedulePlugin schedule = new SchedulePlugin();
        app.use(schedule);
        
        // 路由（从 GuicePlugin 获取实例）
        SpringMvcAnnotationPlugin springMvc = new SpringMvcAnnotationPlugin();
        app.use(springMvc);
        
        app.run();
    }
}
