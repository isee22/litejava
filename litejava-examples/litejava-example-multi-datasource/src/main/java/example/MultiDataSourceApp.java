package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.database.JdbcPlugin;
import litejava.plugins.dataSource.HikariPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多数据源示例 - 主从分离、读写分离
 * 
 * <p>演示如何配置和使用多个数据源：
 * <ul>
 *   <li>primary - 主库（写操作）</li>
 *   <li>secondary - 从库（读操作）</li>
 * </ul>
 * 
 * <p>配置文件 application.yml:
 * <pre>
 * datasource.primary:
 *   url: jdbc:h2:mem:primary
 *   username: sa
 *   password: ""
 * 
 * datasource.secondary:
 *   url: jdbc:h2:mem:secondary
 *   username: sa
 *   password: ""
 * </pre>
 */
public class MultiDataSourceApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // ==================== 配置多数据源（从配置文件读取） ====================
        
        // 主库 - 用于写操作
        HikariPlugin primaryDs = new HikariPlugin("datasource.primary");
        
        // 从库 - 用于读操作
        HikariPlugin secondaryDs = new HikariPlugin("datasource.secondary");
        
        // 使用命名注册（关键！）
        app.use("primary", primaryDs);
        app.use("secondary", secondaryDs);
        
        // 为每个数据源创建 JdbcPlugin
        JdbcPlugin primaryJdbc = new JdbcPlugin(primaryDs);
        JdbcPlugin secondaryJdbc = new JdbcPlugin(secondaryDs);
        app.use("primaryJdbc", primaryJdbc);
        app.use("secondaryJdbc", secondaryJdbc);
        
        // 初始化表结构（两个库都创建）
        initDatabase(primaryJdbc);
        initDatabase(secondaryJdbc);
        
        // 插入测试数据
        primaryJdbc.jdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)", "Primary User", "primary@test.com");
        secondaryJdbc.jdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)", "Secondary User", "secondary@test.com");
        
        // ==================== 路由示例 ====================
        
        // 写操作 - 使用主库
        app.post("/users", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            JdbcPlugin jdbc = app.getPlugin("primaryJdbc", JdbcPlugin.class);
            jdbc.jdbcTemplate.update(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                body.get("name"), body.get("email")
            );
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Created in PRIMARY");
            ctx.ok(result);
        });
        
        // 读操作 - 使用从库
        app.get("/users", ctx -> {
            JdbcPlugin jdbc = app.getPlugin("secondaryJdbc", JdbcPlugin.class);
            List<Map<String, Object>> users = jdbc.jdbcTemplate.queryForList("SELECT * FROM users");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("source", "SECONDARY");
            result.put("users", users);
            ctx.ok(result);
        });
        
        // 查看两个库的数据对比
        app.get("/compare", ctx -> {
            JdbcPlugin primary = app.getPlugin("primaryJdbc", JdbcPlugin.class);
            JdbcPlugin secondary = app.getPlugin("secondaryJdbc", JdbcPlugin.class);
            
            List<Map<String, Object>> primaryUsers = primary.jdbcTemplate.queryForList("SELECT * FROM users");
            List<Map<String, Object>> secondaryUsers = secondary.jdbcTemplate.queryForList("SELECT * FROM users");
            
            Map<String, Object> primaryData = new LinkedHashMap<>();
            primaryData.put("count", primaryUsers.size());
            primaryData.put("users", primaryUsers);
            
            Map<String, Object> secondaryData = new LinkedHashMap<>();
            secondaryData.put("count", secondaryUsers.size());
            secondaryData.put("users", secondaryUsers);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("primary", primaryData);
            result.put("secondary", secondaryData);
            ctx.ok(result);
        });
        
        // 首页
        app.get("/", ctx -> {
            Map<String, Object> endpoints = new LinkedHashMap<>();
            endpoints.put("POST /users", "写入主库");
            endpoints.put("GET /users", "从从库读取");
            endpoints.put("GET /compare", "对比两个库的数据");
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "Multi DataSource Example");
            result.put("endpoints", endpoints);
            ctx.ok(result);
        });
        
        app.run();
    }
    
    private static void initDatabase(JdbcPlugin jdbc) {
        jdbc.jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "  name VARCHAR(100)," +
            "  email VARCHAR(100)" +
            ")"
        );
    }
}
