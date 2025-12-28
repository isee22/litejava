package litejava.plugins.database;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import litejava.Plugin;
import litejava.plugins.dataSource.DataSourcePlugin;

/**
 * JDBC 插件 - 基于 Spring JdbcTemplate
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * datasource:
 *   url: jdbc:mysql://localhost:3306/mydb
 *   username: root
 *   password: root
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 单数据源
 * HikariPlugin hikari = new HikariPlugin();
 * app.use(hikari);
 * app.use(new JdbcPlugin(hikari));
 * 
 * // 多数据源
 * HikariPlugin primary = new HikariPlugin("datasource.primary");
 * HikariPlugin secondary = new HikariPlugin("datasource.secondary");
 * app.use(primary);
 * app.use(secondary);
 * app.use("primaryJdbc", new JdbcPlugin(primary));
 * app.use("secondaryJdbc", new JdbcPlugin(secondary));
 * 
 * // 查询
 * JdbcPlugin jdbc = app.getPlugin(JdbcPlugin.class);
 * List<Map<String, Object>> users = jdbc.jdbcTemplate.queryForList("SELECT * FROM users");
 * 
 * // 事务
 * jdbc.txTemplate.execute(status -> {
 *     jdbc.jdbcTemplate.update("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, fromId);
 *     jdbc.jdbcTemplate.update("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, toId);
 *     return null;
 * });
 * }</pre>
 * 
 * @see DataSourcePlugin 数据源插件基类
 * @see litejava.plugins.dataSource.HikariPlugin HikariCP 数据源
 */
public class JdbcPlugin extends Plugin {
    
    /** 数据源插件 */
    private final DataSourcePlugin dataSourcePlugin;
    
    /** Spring JdbcTemplate */
    public JdbcTemplate jdbcTemplate;
    
    /** Spring TransactionTemplate */
    public TransactionTemplate txTemplate;
    
    /**
     * 构造 JdbcPlugin
     * 
     * @param dataSourcePlugin 数据源插件（必须）
     */
    public JdbcPlugin(DataSourcePlugin dataSourcePlugin) {
        this.dataSourcePlugin = dataSourcePlugin;
    }
    
    @Override
    public void config() {
        DataSource dataSource = dataSourcePlugin.dataSource;
        jdbcTemplate = new JdbcTemplate(dataSource);
        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        
        app.log.info("JdbcPlugin: Ready");
    }
    
    /**
     * 获取数据源
     */
    public DataSource getDataSource() {
        return dataSourcePlugin.dataSource;
    }
    
    @Override
    public void uninstall() {
        // 不关闭 DataSourcePlugin，由 App 统一管理
    }
}
