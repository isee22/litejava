package litejava.plugins.dataSource;

import com.alibaba.druid.pool.DruidDataSource;

import java.sql.SQLException;

/**
 * Druid 数据源插件 - 阿里巴巴连接池，监控功能强
 * 
 * <p>Druid 提供强大的监控和统计功能，适合需要数据库监控的场景。
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
 *     maxWait: 60000
 *     validationQuery: SELECT 1
 *     testOnBorrow: false
 *     testOnReturn: false
 *     testWhileIdle: true
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * DruidPlugin druid = new DruidPlugin();
 * app.use(druid);
 * app.use(new JdbcPlugin(druid));
 * 
 * // 访问 Druid 监控统计
 * druid.druidDataSource.getStatData();
 * }</pre>
 */
public class DruidPlugin extends DataSourcePlugin {
    
    /** Druid 数据源 */
    public DruidDataSource druidDataSource;
    
    // ==================== Druid 特有配置 ====================
    
    /** 获取连接最大等待时间（毫秒） */
    public long maxWait = 60000;
    
    /** 验证查询 SQL */
    public String validationQuery = "SELECT 1";
    
    /** 借出时检测 */
    public boolean testOnBorrow = false;
    
    /** 归还时检测 */
    public boolean testOnReturn = false;
    
    /** 空闲时检测 */
    public boolean testWhileIdle = true;
    
    /** 空闲连接检测间隔（毫秒） */
    public long timeBetweenEvictionRunsMillis = 60000;
    
    /** 连接最小生存时间（毫秒） */
    public long minEvictableIdleTimeMillis = 300000;
    
    public DruidPlugin() {
        super();
    }
    
    public DruidPlugin(String configPrefix) {
        super(configPrefix);
    }
    
    @Override
    public void config() {
        // 加载通用配置
        loadConfig();
        
        // 验证必需配置
        validateConfig();
        
        // 加载 Druid 特有配置
        maxWait = app.conf.getLong(configPrefix + ".pool", "maxWait", maxWait);
        validationQuery = app.conf.getString(configPrefix + ".pool", "validationQuery", validationQuery);
        testOnBorrow = app.conf.getBool(configPrefix + ".pool", "testOnBorrow", testOnBorrow);
        testOnReturn = app.conf.getBool(configPrefix + ".pool", "testOnReturn", testOnReturn);
        testWhileIdle = app.conf.getBool(configPrefix + ".pool", "testWhileIdle", testWhileIdle);
        timeBetweenEvictionRunsMillis = app.conf.getLong(configPrefix + ".pool", "timeBetweenEvictionRunsMillis", timeBetweenEvictionRunsMillis);
        minEvictableIdleTimeMillis = app.conf.getLong(configPrefix + ".pool", "minEvictableIdleTimeMillis", minEvictableIdleTimeMillis);
        
        // 创建 Druid 数据源
        druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        if (driver != null && !driver.isEmpty()) {
            druidDataSource.setDriverClassName(driver);
        }
        druidDataSource.setMaxActive(maxPoolSize);
        druidDataSource.setMinIdle(minIdle);
        druidDataSource.setMaxWait(maxWait);
        druidDataSource.setValidationQuery(validationQuery);
        druidDataSource.setTestOnBorrow(testOnBorrow);
        druidDataSource.setTestOnReturn(testOnReturn);
        druidDataSource.setTestWhileIdle(testWhileIdle);
        druidDataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        druidDataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        
        try {
            druidDataSource.init();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize Druid DataSource", e);
        }
        
        dataSource = druidDataSource;
        
        app.log.info("DruidPlugin: Connected to " + url + " (pool: " + maxPoolSize + ")");
    }
    
    @Override
    public void uninstall() {
        if (druidDataSource != null && !druidDataSource.isClosed()) {
            druidDataSource.close();
        }
    }
}
