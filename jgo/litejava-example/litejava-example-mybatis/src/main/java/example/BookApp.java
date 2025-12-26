package example;

import example.controller.BookController;
import example.mapper.BookMapper;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.security.CorsPlugin;

/**
 * 图书管理系统 - MyBatis 版本
 */
public class BookApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 插件
        app.use(new MyBatisPlugin(BookMapper.class));
        app.use(new CorsPlugin());
        
        // 路由
        app.register(new BookController().routes());
        
        app.run();
    }
}
