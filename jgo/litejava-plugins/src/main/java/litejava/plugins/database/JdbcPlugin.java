package litejava.plugins.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import litejava.Plugin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC 插件 - 基于 HikariCP + Spring JdbcTemplate
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.zaxxer</groupId>
 *     <artifactId>HikariCP</artifactId>
 *     <version>4.0.3</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.springframework</groupId>
 *     <artifactId>spring-jdbc</artifactId>
 *     <version>5.3.31</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * jdbc.url=jdbc:mysql://localhost:3306/mydb
 * jdbc.username=root
 * jdbc.password=root
 * jdbc.pool.maxSize=10
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 注册插件
 * JdbcPlugin jdbc = new JdbcPlugin();
 * app.use(jdbc);
 * 
 * // 查询
 * List<Map<String, Object>> users = jdbc.jdbcTemplate.queryForList("SELECT * FROM users");
 * 
 * // 带参数查询
 * User user = jdbc.jdbcTemplate.queryForObject(
 *     "SELECT * FROM users WHERE id = ?",
 *     (rs, i) -> {
 *         User u = new User();
 *         u.id = rs.getLong("id");
 *         u.name = rs.getString("name");
 *         return u;
 *     },
 *     userId
 * );
 * 
 * // 插入/更新
 * jdbc.jdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)", name, email);
 * 
 * // 事务
 * jdbc.txTemplate.execute(status -> {
 *     jdbc.jdbcTemplate.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, fromId);
 *     jdbc.jdbcTemplate.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, toId);
 *     return null;
 * });
 * 
 * // 多数据源
 * JdbcPlugin primary = new JdbcPlugin("jdbc.primary");
 * JdbcPlugin secondary = new JdbcPlugin("jdbc.secondary");
 * app.use(primary);
 * app.use(secondary);
 * }</pre>
 * 
 * <h2>vs 其他数据库插件</h2>
 * <ul>
 *   <li>JdbcPlugin - 轻量级，直接写 SQL，适合简单场景</li>
 *   <li>MyBatisPlugin - SQL 映射，适合复杂查询</li>
 *   <li>JpaPlugin/HibernatePlugin - ORM，适合领域模型</li>
 * </ul>
 */
public class JdbcPlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static JdbcPlugin instance;
    
    /** HikariCP 数据源 */
    public HikariDataSource dataSource;
    
    /** Spring JdbcTemplate，用于执行 SQL */
    public JdbcTemplate jdbcTemplate;
    
    /** Spring TransactionTemplate，用于事务管理 */
    public TransactionTemplate txTemplate;
    
    /** 配置前缀，默认 "jdbc" */
    public String configPrefix = "jdbc";
    
    /**
     * 创建 JdbcPlugin 实例，自动检测是否使用虚拟线程版本
     * 需要 Java 21+ 且 classpath 中有 JdbcVirtualThreadPlugin
     * 
     * @param useVirtualThreads 是否使用虚拟线程
     * @return JdbcPlugin 或 JdbcVirtualThreadPlugin 实例
     */
    public static JdbcPlugin create(boolean useVirtualThreads) {
        return create(useVirtualThreads, "jdbc");
    }
    
    /**
     * 创建 JdbcPlugin 实例，自动检测是否使用虚拟线程版本
     * 
     * @param useVirtualThreads 是否使用虚拟线程
     * @param configPrefix 配置前缀
     * @return JdbcPlugin 或 JdbcVirtualThreadPlugin 实例
     */
    public static JdbcPlugin create(boolean useVirtualThreads, String configPrefix) {
        if (useVirtualThreads) {
            try {
                Class<?> vtClass = Class.forName("litejava.plugins.vt.JdbcVirtualThreadPlugin");
                return (JdbcPlugin) vtClass.getConstructor(String.class).newInstance(configPrefix);
            } catch (Exception e) {
                throw new RuntimeException("JdbcVirtualThreadPlugin not found. Add litejava-plugins-vt dependency.", e);
            }
        }
        return new JdbcPlugin(configPrefix);
    }
    
    public JdbcPlugin() {}
    
    public JdbcPlugin(String configPrefix) {
        this.configPrefix = configPrefix;
    }
    
    @Override
    public void config() {
        HikariConfig config = createHikariConfig();
        
        dataSource = new HikariDataSource(config);
        jdbcTemplate = new JdbcTemplate(dataSource);
        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        
        if (instance == null) instance = this;
        app.log.info("JdbcPlugin configured (" + configPrefix + ")");
    }
    
    /**
     * 创建 HikariConfig，子类可覆盖以自定义配置
     */
    protected HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(app.conf.getString(configPrefix, "url", null));
        config.setUsername(app.conf.getString(configPrefix, "username", null));
        config.setPassword(app.conf.getString(configPrefix, "password", null));
        config.setMaximumPoolSize(app.conf.getInt(configPrefix + ".pool", "maxSize", 10));
        return config;
    }
    
    @Override
    public void uninstall() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
