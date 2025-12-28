package litejava.plugins.vt;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import litejava.plugins.dataSource.HikariPlugin;

/**
 * HikariCP 虚拟线程版本 (Java 21+)
 * 
 * <p>继承 HikariPlugin，使用虚拟线程工厂配置 HikariCP，
 * 适合高并发场景下的数据库连接池。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * HikariVirtualThreadPlugin hikari = new HikariVirtualThreadPlugin();
 * app.use(hikari);
 * app.use(new JdbcPlugin(hikari));
 * }</pre>
 */
public class HikariVirtualThreadPlugin extends HikariPlugin {
    
    public HikariVirtualThreadPlugin() {
        super();
    }
    
    public HikariVirtualThreadPlugin(String configPrefix) {
        super(configPrefix);
    }
    
    @Override
    public void config() {
        // 加载通用配置
        loadConfig();
        
        // 加载 HikariCP 特有配置
        connectionTimeout = app.conf.getLong(configPrefix + ".pool", "connectionTimeout", connectionTimeout);
        idleTimeout = app.conf.getLong(configPrefix + ".pool", "idleTimeout", idleTimeout);
        maxLifetime = app.conf.getLong(configPrefix + ".pool", "maxLifetime", maxLifetime);
        
        if (url == null || url.isEmpty()) {
            app.log.warn("HikariVirtualThreadPlugin: No URL configured, skipping initialization");
            return;
        }
        
        // 创建 HikariCP 数据源（使用虚拟线程）
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
        
        // 使用虚拟线程工厂
        config.setThreadFactory(Thread.ofVirtual().factory());
        
        hikariDataSource = new HikariDataSource(config);
        dataSource = hikariDataSource;
        
        app.log.info("HikariVirtualThreadPlugin: Connected to " + url + " (pool: " + maxPoolSize + ", virtual threads)");
    }
}
