package example;

import example.controller.AuthController;
import example.controller.BookController;
import example.controller.FileController;
import example.infra.Db;
import litejava.App;
import litejava.plugin.StaticFilePlugin;
import litejava.plugins.LiteJava;
import litejava.plugins.database.JdbcPlugin;
import litejava.plugins.dataSource.HikariPlugin;
import litejava.plugins.log.RequestLogPlugin;
import litejava.plugins.security.AuthPlugin;

import java.util.LinkedHashMap;
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
        HikariPlugin hikari = new HikariPlugin();
        app.use(hikari);
        JdbcPlugin jdbc = new JdbcPlugin(hikari);
        app.use(jdbc);
        
        // 初始化 Db 工具类
        Db.init(jdbc);
        
        // 静态文件
        app.use(new StaticFilePlugin("/static", "static"));
        
        // 请求日志
        app.use(new RequestLogPlugin());
        
        // Token 认证（数据库存储）
        app.use(new AuthPlugin(token -> {
            String username = Dao.session.findUsernameByToken(token);
            if (username != null) {
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("username", username);
                return user;
            }
            return null;
        }).whitelist("/", "/api/auth/login", "/api/auth/register")
          .whitelistPrefix("/static", "/api/books"));
        
        // 配置 FilePlugin
        app.file.uploadDir(app.conf.getString("upload", "dir", "./uploads"));
        app.file.maxFileSize(app.conf.getLong("upload", "maxSize", 10485760L));
        
        // 路由
        app.register(new AuthController().routes());
        app.register(new BookController().routes());
        app.register(new FileController(
            app.conf.getString("upload", "subDir", "covers")
        ).routes());
        
        app.run();
    }
}
