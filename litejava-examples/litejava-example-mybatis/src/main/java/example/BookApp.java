package example;

import example.controller.BookController;
import example.mapper.BookMapper;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.MyBatisPlugin;
import litejava.plugins.dataSource.HikariPlugin;
import litejava.plugins.security.CorsPlugin;

/**
 * 图书管理系统 - MyBatis 版本
 */
public class BookApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 数据源
        HikariPlugin hikari = new HikariPlugin();
        app.use(hikari);
        
        // MyBatis
        MyBatisPlugin mybatis = new MyBatisPlugin(hikari, BookMapper.class);
        app.use(mybatis);
        app.use(new CorsPlugin());
        
        // 初始化 Services
        Services.init(mybatis);
        
        // 路由
        app.register(new BookController().routes());
        
        app.run();
    }
}
