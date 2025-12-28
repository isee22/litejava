package example;

import example.service.UserService;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.annotation.SpringMvcAnnotationPlugin;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.dataSource.HikariPlugin;
import litejava.plugins.di.GuicePlugin;
import litejava.plugins.http.RecoveryPlugin;
import litejava.plugins.schedule.SchedulePlugin;
import litejava.plugins.view.ThymeleafPlugin;

/**
 * LiteJava Spring Boot 风格示例 - 编程式配置
 * 
 * 定时任务：编程式 schedule.cron()
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
        
        // DI（绑定 Mapper）
        GuicePlugin di = new GuicePlugin();
        di.bind(binder -> {
            for (Class<?> mapperClass : mybatis.getMapperClasses()) {
                binder.bind((Class<Object>) mapperClass).toInstance(mybatis.getMapper(mapperClass));
            }
        });
        app.use(di);
        
        // 定时任务（编程式）
        SchedulePlugin schedule = new SchedulePlugin();
        app.use(schedule);
        
        app.onStarted(() -> {
            UserService userService = di.get(UserService.class);
            schedule.cron("0 * * * * ?", () -> {
                System.out.println("[Scheduler] User count: " + userService.findAll().size());
            });
            app.log.info("Programmatic scheduled task registered");
        });
        
        // 路由
        SpringMvcAnnotationPlugin springMvc = new SpringMvcAnnotationPlugin();
        springMvc.packages = "example.controller";
        app.use(springMvc);
        
        app.run();
    }
}
