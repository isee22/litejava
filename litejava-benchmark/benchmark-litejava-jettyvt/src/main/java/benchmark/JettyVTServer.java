package benchmark;

import litejava.App;
import litejava.plugin.ConfPlugin;
import litejava.plugin.StaticFilePlugin;
import litejava.plugins.database.JdbcPlugin;
import litejava.plugins.dataSource.HikariPlugin;
import litejava.plugins.json.GoJsonPlugin;
import litejava.plugins.log.Slf4jLogPlugin;
import litejava.plugins.view.ThymeleafPlugin;
import litejava.plugins.vt.JettyVirtualThreadPlugin;

import java.util.*;

public class JettyVTServer {
    
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        App app = new App();
        app.devMode(false);
        app.use(new Slf4jLogPlugin());
        app.use(new ConfPlugin("benchmark.properties"));
        app.use(new GoJsonPlugin());
        
        HikariPlugin hikari = new HikariPlugin("db");
        app.use(hikari);
        JdbcPlugin jdbc = new JdbcPlugin(hikari);
        app.use(jdbc);
        
        // 端口通过配置文件 server.port 设置，或 use 后覆盖
        JettyVirtualThreadPlugin server = new JettyVirtualThreadPlugin();
        app.use(server);
        server.port = 8187;  // config() 已执行，直接覆盖
        app.use(new ThymeleafPlugin("templates/"));
        app.use(new StaticFilePlugin("/static", "static"));
        
        app.get("/text", ctx -> ctx.text("Hello, World!"));
        
        app.get("/json", ctx -> ctx.json(Map.of(
            "message", "Hello, World!",
            "framework", "LiteJava-JettyVT",
            "timestamp", System.currentTimeMillis()
        )));
        
        app.get("/dynamic", ctx -> ctx.render("users", Map.of(
            "framework", "LiteJava-JettyVT",
            "users", jdbc.jdbcTemplate.queryForList("SELECT * FROM users ORDER BY id LIMIT 10")
        )));
        
        app.get("/users", ctx -> {
            int page = parseInt(ctx.queryParam("page"), 1);
            int size = Math.min(parseInt(ctx.queryParam("size"), 50), 100);
            ctx.json(Map.of(
                "data", jdbc.jdbcTemplate.queryForList("SELECT * FROM users ORDER BY id LIMIT ? OFFSET ?", size, (page - 1) * size),
                "page", page, "size", size
            ));
        });
        
        app.get("/posts", ctx -> {
            int page = parseInt(ctx.queryParam("page"), 1);
            int size = Math.min(parseInt(ctx.queryParam("size"), 50), 100);
            ctx.json(Map.of(
                "data", jdbc.jdbcTemplate.queryForList("SELECT p.*, u.name as author FROM posts p LEFT JOIN users u ON p.user_id = u.id ORDER BY p.id LIMIT ? OFFSET ?", size, (page - 1) * size),
                "page", page, "size", size
            ));
        });
        
        app.run();
        System.out.println("JettyVT started in " + (System.currentTimeMillis() - start) + "ms on port " + server.port);
    }
    
    private static int parseInt(String s, int def) { return s != null ? Integer.parseInt(s) : def; }
}
