package benchmark;

import litejava.App;
import litejava.plugin.ConfPlugin;
import litejava.plugin.StaticFilePlugin;
import litejava.plugins.database.JdbcPlugin;
import litejava.plugins.json.JacksonPlugin;
import litejava.plugins.log.Slf4jLogPlugin;
import litejava.plugins.server.JettyServerPlugin;
import litejava.plugins.view.ThymeleafPlugin;

import java.util.*;

public class LiteJavaServer {
    
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        App app = new App();
        app.devMode(false);
        app.use(new Slf4jLogPlugin());
        app.use(new ConfPlugin("benchmark.properties"));
        app.use(new JacksonPlugin());
        
        JdbcPlugin jdbc = new JdbcPlugin("db");
        app.use(jdbc);
        app.use(new JettyServerPlugin());
        app.use(new ThymeleafPlugin("templates/"));
        app.use(new StaticFilePlugin("/static", "static"));
        
        // 使用预编码 byte[]，零拷贝
        app.get("/text", ctx -> ctx.text("Hello, World!"));
        
        app.get("/json", ctx -> ctx.json(Map.of(
            "message", "Hello, World!",
            "framework", "LiteJava",
            "timestamp", System.currentTimeMillis()
        )));
        
        app.get("/dynamic", ctx -> ctx.render("users.html", Map.of(
            "framework", "LiteJava",
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
        
        int port = app.conf.getInt("server", "litejava.port", 8181);
        app.run(port);
        System.out.println("LiteJava started in " + (System.currentTimeMillis() - start) + "ms on port " + port);
    }
    
    private static int parseInt(String s, int def) { return s != null ? Integer.parseInt(s) : def; }
}
