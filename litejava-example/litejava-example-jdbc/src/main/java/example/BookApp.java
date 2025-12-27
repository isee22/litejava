package example;

import example.controller.AuthController;
import example.controller.BookController;
import example.controller.FileController;
import litejava.App;
import litejava.plugin.StaticFilePlugin;
import litejava.plugins.LiteJava;
import litejava.plugins.database.JdbcPlugin;
import litejava.plugins.log.RequestLogPlugin;
import litejava.plugins.security.AuthPlugin;

import java.util.Map;

/**
 * 图书管理系统示例
 * 
 * <p>演示两种认证方式：
 * <ul>
 *   <li>Token 认证：前后端分离，token 存 Authorization header</li>
 *   <li>Session 认证：传统 Web，session 存 Cookie</li>
 * </ul>
 * 
 * <p>本示例使用 Token 认证（数据库存储），适合 API 服务。
 * 如需 Session 认证，可改用 SessionPlugin。
 */
public class BookApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 数据库
        app.use(new JdbcPlugin());
        
        // 静态文件
        app.use(new StaticFilePlugin("/static", "static"));
        
        // 请求日志
        app.use(new RequestLogPlugin());
        
        // Token 认证（数据库存储）
        app.use(new AuthPlugin(token -> {
            String username = Dao.session.findUsernameByToken(token);
            return username != null ? Map.of("username", username) : null;
        }).whitelist("/", "/api/auth/login", "/api/auth/register")
          .whitelistPrefix("/static", "/api/books"));
        
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
