package benchmark;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.*;
import java.util.*;

/**
 * Javalin Benchmark Server - Virtual Threads enabled
 */
public class JavalinServer {
    
    private static Properties conf = new Properties();
    private static JdbcTemplate jdbc;
    private static TemplateEngine templateEngine;
    
    public static void main(String[] args) {
        loadConfig();
        initDatabase();
        initTemplateEngine();
        
        int port = Integer.parseInt(conf.getProperty("server.javalin.port", "8182"));
        
        long start = System.currentTimeMillis();
        Javalin app = Javalin.create(config -> {
            config.useVirtualThreads = true;
            config.staticFiles.add(sf -> {
                sf.hostedPath = "/static";
                sf.directory = "/static";
                sf.location = Location.CLASSPATH;
            });
        }).start(port);
        System.out.println("Javalin started in " + (System.currentTimeMillis() - start) + "ms on port " + port);
        
        app.get("/text", ctx -> ctx.result("Hello, World!"));
        
        app.get("/json", ctx -> ctx.json(Map.of(
            "message", "Hello, World!",
            "framework", "Javalin",
            "timestamp", System.currentTimeMillis()
        )));
        
        app.get("/dynamic", ctx -> {
            Context thymeleafCtx = new Context();
            thymeleafCtx.setVariable("framework", "Javalin");
            thymeleafCtx.setVariable("users", findUsers(1, 10));
            ctx.html(templateEngine.process("users", thymeleafCtx));
        });
        
        app.get("/users", ctx -> {
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(50);
            ctx.json(Map.of("data", findUsers(page, size), "page", page, "size", size));
        });
        
        app.get("/posts", ctx -> {
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(50);
            ctx.json(Map.of("data", findPosts(page, size), "page", page, "size", size));
        });
    }
    
    private static void loadConfig() {
        String[] paths = { "benchmark.properties", "../benchmark.properties" };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) { conf.load(fis); return; } catch (IOException e) {}
            }
        }
    }
    
    private static void initTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(true);
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }
    
    private static void initDatabase() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(conf.getProperty("db.url", "jdbc:mysql://localhost:3306/benchmark"));
        config.setUsername(conf.getProperty("db.username", "root"));
        config.setPassword(conf.getProperty("db.password", "123456"));
        config.setMaximumPoolSize(Integer.parseInt(conf.getProperty("db.pool.maxSize", "10")));
        config.setMinimumIdle(5);
        jdbc = new JdbcTemplate(new HikariDataSource(config));
    }
    
    private static List<Map<String, Object>> findUsers(int page, int size) {
        return jdbc.queryForList("SELECT * FROM users ORDER BY id LIMIT ? OFFSET ?", Math.min(size, 100), (page - 1) * size);
    }
    
    private static List<Map<String, Object>> findPosts(int page, int size) {
        return jdbc.queryForList("SELECT p.*, u.name as author FROM posts p LEFT JOIN users u ON p.user_id = u.id ORDER BY p.id LIMIT ? OFFSET ?", Math.min(size, 100), (page - 1) * size);
    }
}
