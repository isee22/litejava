package litejava.plugins.dataSource;

import litejava.Plugin;

import javax.sql.DataSource;

/**
 * 数据源插件基类 - 提供数据库连接池管理
 * 
 * <p>这是数据库的基础插件，提供连接池管理。其他需要数据库的插件
 * （如 JdbcPlugin、MyBatisPlugin）需要在构造时传入此插件实例。
 * 
 * <h2>实现类</h2>
 * <ul>
 *   <li>{@link HikariPlugin} - HikariCP 连接池（推荐，高性能）</li>
 *   <li>{@link DruidPlugin} - Druid 连接池（阿里巴巴，监控功能强）</li>
 * </ul>
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * datasource:
 *   url: jdbc:mysql://localhost:3306/mydb
 *   username: root
 *   password: root
 *   driver: com.mysql.cj.jdbc.Driver
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用 HikariCP（推荐）
 * HikariPlugin hikari = new HikariPlugin();
 * app.use(hikari);
 * app.use(new JdbcPlugin(hikari));
 * 
 * // 使用 Druid
 * DruidPlugin druid = new DruidPlugin();
 * app.use(druid);
 * app.use(new MyBatisPlugin(druid));
 * }</pre>
 */
public abstract class DataSourcePlugin extends Plugin {
    
    /** 数据源 */
    public DataSource dataSource;
    
    /** 配置前缀 */
    public String configPrefix = "datasource";
    
    // ==================== 通用配置字段 ====================
    
    /** JDBC URL */
    public String url;
    
    /** 用户名 */
    public String username;
    
    /** 密码 */
    public String password;
    
    /** 驱动类名（可选，连接池会自动检测） */
    public String driver;
    
    /** 最大连接数 */
    public int maxPoolSize = 20;
    
    /** 最小空闲连接数 */
    public int minIdle = 5;
    
    public DataSourcePlugin() {
    }
    
    /**
     * 使用指定配置前缀（支持多数据源）
     */
    public DataSourcePlugin(String configPrefix) {
        this.configPrefix = configPrefix;
    }
    
    /**
     * 加载通用配置
     */
    protected void loadConfig() {
        url = app.conf.getString(configPrefix, "url", url);
        username = app.conf.getString(configPrefix, "username", username);
        password = app.conf.getString(configPrefix, "password", password);
        driver = app.conf.getString(configPrefix, "driver", driver);
        maxPoolSize = app.conf.getInt(configPrefix + ".pool", "maxSize", maxPoolSize);
        minIdle = app.conf.getInt(configPrefix + ".pool", "minIdle", minIdle);
    }
    
    /**
     * 验证必需配置
     * @throws litejava.exception.LiteJavaException 如果必需配置缺失
     */
    protected void validateConfig() {
        if (url == null || url.isEmpty()) {
            throw new litejava.exception.LiteJavaException(
                getClass().getSimpleName() + ": Missing required configuration '" + configPrefix + ".url'");
        }
    }
    
    /**
     * 获取数据源
     */
    public DataSource getDataSource() {
        return dataSource;
    }
}
