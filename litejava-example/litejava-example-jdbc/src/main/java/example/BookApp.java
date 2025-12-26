package example;

import example.controller.AuthController;
import example.controller.BookController;
import example.controller.FileController;
import example.infra.AuthMiddleware;
import litejava.App;
import litejava.plugin.StaticFilePlugin;
import litejava.plugins.LiteJava;
import litejava.plugins.database.JdbcPlugin;
import litejava.plugins.log.RequestLogPlugin;

/**
 * 图书管理系统示例
 */
public class BookApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 额外插件
        app.use(new JdbcPlugin());
        app.use(new StaticFilePlugin("/static", "static"));
        
        // 中间件
        app.use(new RequestLogPlugin());
        app.use(new AuthMiddleware());
        
        // 路由
        app.register(new AuthController().routes());
        app.register(new BookController().routes());
        app.register(new FileController(
            app.conf.getString("upload", "dir", "uploads"),
            app.conf.getLong("upload", "maxSize", 10485760L)
        ).routes());
        
        app.run();
    }
}
