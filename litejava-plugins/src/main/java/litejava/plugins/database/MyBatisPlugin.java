package litejava.plugins.database;

import java.util.function.Function;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import litejava.Plugin;
import litejava.plugins.dataSource.DataSourcePlugin;

/**
 * MyBatis 插件 - 配置、初始化和便捷操作
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * datasource:
 *   url: jdbc:mysql://localhost:3306/mydb
 *   username: root
 *   password: root
 * mybatis:
 *   mapperPackage: com.example.mapper
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 单数据源
 * HikariPlugin hikari = new HikariPlugin();
 * app.use(hikari);
 * app.use(new MyBatisPlugin(hikari));
 * 
 * // 指定 Mapper 类
 * app.use(new MyBatisPlugin(hikari, UserMapper.class, OrderMapper.class));
 * 
 * // 多数据源
 * HikariPlugin primary = new HikariPlugin("datasource.primary");
 * HikariPlugin secondary = new HikariPlugin("datasource.secondary");
 * app.use(primary);
 * app.use(secondary);
 * app.use("primaryMybatis", new MyBatisPlugin(primary));
 * app.use("secondaryMybatis", new MyBatisPlugin(secondary));
 * 
 * // 执行查询
 * MyBatisPlugin mybatis = app.getPlugin(MyBatisPlugin.class);
 * User user = mybatis.execute(UserMapper.class, m -> m.findById(1));
 * 
 * // 事务
 * mybatis.tx(session -> {
 *     UserMapper mapper = session.getMapper(UserMapper.class);
 *     mapper.insert(user1);
 *     mapper.insert(user2);
 *     return null;
 * });
 * }</pre>
 * 
 * @see DataSourcePlugin 数据源插件基类
 * @see litejava.plugins.dataSource.HikariPlugin HikariCP 数据源
 */
public class MyBatisPlugin extends Plugin {
    
    /** 数据源插件 */
    private final DataSourcePlugin dataSourcePlugin;
    
    /** MyBatis SqlSessionFactory */
    public SqlSessionFactory sqlSessionFactory;
    
    /** Mapper 类（可选） */
    private Class<?>[] mapperClasses;
    
    /**
     * 构造 MyBatisPlugin
     * 
     * @param dataSourcePlugin 数据源插件（必须）
     */
    public MyBatisPlugin(DataSourcePlugin dataSourcePlugin) {
        this.dataSourcePlugin = dataSourcePlugin;
    }
    
    /**
     * 构造 MyBatisPlugin 并指定 Mapper 类
     * 
     * @param dataSourcePlugin 数据源插件（必须）
     * @param mapperClasses Mapper 接口类
     */
    public MyBatisPlugin(DataSourcePlugin dataSourcePlugin, Class<?>... mapperClasses) {
        this.dataSourcePlugin = dataSourcePlugin;
        this.mapperClasses = mapperClasses;
    }
    
    @Override
    public void config() {
        DataSource dataSource = dataSourcePlugin.dataSource;
        
        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("default", 
            new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        
        // 从配置文件注册 mapper 包
        String mapperPackage = app.conf.getString("mybatis", "mapperPackage", null);
        if (mapperPackage != null) {
            for (String pkg : mapperPackage.split(",")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    configuration.addMappers(trimmed);
                }
            }
        }
        
        // 从构造参数注册 mapper 类
        if (mapperClasses != null) {
            for (Class<?> mapperClass : mapperClasses) {
                configuration.addMapper(mapperClass);
            }
        }
        
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        app.log.info("MyBatisPlugin: Ready");
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
    
    /**
     * 注册 Mapper 接口
     */
    public void addMapper(Class<?> mapperClass) {
        sqlSessionFactory.getConfiguration().addMapper(mapperClass);
    }
    
    /**
     * 执行 Mapper 操作（自动管理 Session）
     */
    public <T, R> R execute(Class<T> mapperClass, Function<T, R> action) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            T mapper = session.getMapper(mapperClass);
            return action.apply(mapper);
        }
    }
    
    /**
     * 执行事务操作（手动提交）
     */
    public <R> R tx(Function<SqlSession, R> action) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                R result = action.apply(session);
                session.commit();
                return result;
            } catch (Exception e) {
                session.rollback();
                throw e;
            }
        }
    }
    
    /**
     * 获取 Mapper 实例（用于 DI 绑定）
     */
    public <T> T getMapper(Class<T> mapperClass) {
        return sqlSessionFactory.openSession(true).getMapper(mapperClass);
    }
    
    /**
     * 获取所有已注册的 Mapper 类
     */
    public java.util.Collection<Class<?>> getMapperClasses() {
        return sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers();
    }
}
