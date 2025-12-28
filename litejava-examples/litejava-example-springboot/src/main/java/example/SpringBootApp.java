package example;

import example.service.UserService;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.annotation.SpringMvcAnnotationPlugin;
import litejava.plugins.cache.MemoryCachePlugin;
import litejava.plugins.cache.SpringCachePlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.dataSource.HikariPlugin;
import litejava.plugins.di.GuicePlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.schedule.SchedulePlugin;
import litejava.plugins.view.ThymeleafPlugin;

/**
 * LiteJava Spring Boot 风格示例
 * 
 * 包含：Spring MVC 注解、Guice DI、MyBatis、Spring Cache、定时任务
 */
public class SpringBootApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 中间件
        app.use(RecoveryPlugin.withStack());
        app.use(new ThymeleafPlugin("templates/"));
        
        // 数据库
        HikariPlugin hikari = new HikariPlugin();
        MyBatisPlugin mybatis = new MyBatisPlugin(hikari);
        app.use(hikari);
        app.use(mybatis);
        
        // 缓存 + Spring Cache 注解支持
        MemoryCachePlugin cache = new MemoryCachePlugin();
        SpringCachePlugin springCache = new SpringCachePlugin(cache);
        app.use(cache);
        app.use(springCache);
        
        // DI
        GuicePlugin di = new GuicePlugin();
        di.bind(binder -> {
            for (Class<?> mapper : mybatis.getMapperClasses()) {
                binder.bind((Class<Object>) mapper).toInstance(mybatis.getMapper(mapper));
            }
            binder.bind(SpringCachePlugin.class).toInstance(springCache);
        });
        app.use(di);
        
        // 定时任务
        SchedulePlugin schedule = new SchedulePlugin();
        app.use(schedule);
        app.onStarted(() -> {
            UserService userService = di.get(UserService.class);
            schedule.cron("0 * * * * ?", () -> 
                System.out.println("[Scheduler] User count: " + userService.findAll().size())
            );
        });
        
        // Spring MVC 注解路由
        SpringMvcAnnotationPlugin springMvc = new SpringMvcAnnotationPlugin();
        springMvc.packages = "example.controller";
        app.use(springMvc);
        
        app.run();
    }
}
