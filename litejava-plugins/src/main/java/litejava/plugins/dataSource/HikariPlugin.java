package litejava.plugins.dataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * HikariCP 数据源插件 - 高性能连接池（推荐）
 * 
 * <p>HikariCP 是目前最快的 Java 连接池，Spring Boot 2.x 默认使用。
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * datasource:
 *   url: jdbc:mysql://localhost:3306/mydb
 *   username: root
 *   password: root
 *   driver: com.mysql.cj.jdbc.Driver
 *   pool:
 *     maxSize: 20
 *     minIdle: 5
 *     connectionTimeout: 30000
 *     idleTimeout: 600000
 *     maxLifetime: 1800000
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * HikariPlugin hikari = new HikariPlugin();
 * app.use(hikari);
 * app.use(new JdbcPlugin(hikari));
 * 
 * // 多数据源
 * HikariPlugin primary = new HikariPlugin("datasource.primary");
 * HikariPlugin secondary = new HikariPlugin("datasource.secondary");
 * app.use(primary);
 * app.use(secondary);
 * app.use(new JdbcPlugin(primary));
 * app.use(new MyBatisPlugin(secondary));
 * }</pre>
 */
public class HikariPlugin extends DataSourcePlugin {
    
    /** HikariCP 数据源 */
    public HikariDataSource hikariDataSource;
    
    // ==================== HikariCP 特有配置 ====================
    
    /** 连接超时（毫秒） */
    public long connectionTimeout = 30000;
    
    /** 空闲超时（毫秒） */
    public long idleTimeout = 600000;
    
    /** 连接最大生命周期（毫秒） */
    public long maxLifetime = 1800000;
    
    public HikariPlugin() {
        super();
    }
    
    public HikariPlugin(String configPrefix) {
        super(configPrefix);
    }
    
    @Override
    public void config() {
        // 加载通用配置
        loadConfig();
        
        // 验证必需配置
        validateConfig();
        
        // 加载 HikariCP 特有配置
        connectionTimeout = app.conf.getLong(configPrefix + ".pool", "connectionTimeout", connectionTimeout);
        idleTimeout = app.conf.getLong(configPrefix + ".pool", "idleTimeout", idleTimeout);
        maxLifetime = app.conf.getLong(configPrefix + ".pool", "maxLifetime", maxLifetime);
        
        // 创建 HikariCP 数据源
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null && !driver.isEmpty()) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        hikariDataSource = new HikariDataSource(config);
        dataSource = hikariDataSource;
        
        app.log.info("HikariPlugin: Connected to " + url + " (pool: " + maxPoolSize + ")");
    }
    
    @Override
    public void uninstall() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
    }
}
