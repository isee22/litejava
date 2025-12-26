package litejava.plugins.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import litejava.Plugin;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.util.function.Function;

/**
 * MyBatis 插件 - 配置、初始化和便捷操作
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * mybatis.url=jdbc:mysql://localhost:3306/mydb
 * mybatis.username=root
 * mybatis.password=123456
 * mybatis.mapperPackage=com.example.mapper
 * mybatis.pool.maxSize=10
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 注册插件
 * MyBatisPlugin mybatis = new MyBatisPlugin();
 * app.use(mybatis);
 * 
 * // 方式1: 使用实例方法 execute (推荐)
 * User user = MyBatisPlugin.instance.execute(UserMapper.class, m -> m.findById(1));
 * List<User> users = MyBatisPlugin.instance.execute(UserMapper.class, UserMapper::findAll);
 * 
 * // 方式2: 使用事务 tx
 * MyBatisPlugin.instance.tx(session -> {
 *     UserMapper mapper = session.getMapper(UserMapper.class);
 *     mapper.insert(user1);
 *     mapper.insert(user2);
 *     return null;
 * });
 * 
 * // 方式3: 直接使用 SqlSessionFactory
 * try (SqlSession session = mybatis.sqlSessionFactory.openSession()) {
 *     UserMapper mapper = session.getMapper(UserMapper.class);
 *     User user = mapper.findById(1);
 * }
 * }</pre>
 */
public class MyBatisPlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static MyBatisPlugin instance;
    
    /** HikariCP 数据源，初始化后可用 */
    public HikariDataSource dataSource;
    
    /** MyBatis SqlSessionFactory，初始化后可用 */
    public SqlSessionFactory sqlSessionFactory;
    
    /** Mapper 类（可选，用于构造时注册） */
    private Class<?>[] mapperClasses;
    
    public MyBatisPlugin() {
    }
    
    /**
     * 构造时指定 Mapper 类
     */
    public MyBatisPlugin(Class<?>... mapperClasses) {
        this.mapperClasses = mapperClasses;
    }
    
    @Override
    public void config() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(app.conf.getString("mybatis", "url", null));
        hikariConfig.setUsername(app.conf.getString("mybatis", "username", null));
        hikariConfig.setPassword(app.conf.getString("mybatis", "password", null));
        hikariConfig.setMaximumPoolSize(app.conf.getInt("mybatis", "poolMaxSize", 10));
        
        dataSource = new HikariDataSource(hikariConfig);
        
        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("default", 
            new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        
        // 从配置文件注册 mapper 包
        String mapperPackage = app.conf.getString("mybatis", "mapperPackage", null);
        if (mapperPackage != null) {
            configuration.addMappers(mapperPackage);
        }
        
        // 从构造参数注册 mapper 类
        if (mapperClasses != null) {
            for (Class<?> mapperClass : mapperClasses) {
                configuration.addMapper(mapperClass);
            }
        }
        
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        if (instance == null) instance = this;
        app.log.info("MyBatisPlugin configured");
    }
    
    @Override
    public void uninstall() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
    
    /**
     * 注册 Mapper 接口
     */
    public void addMapper(Class<?> mapperClass) {
        sqlSessionFactory.getConfiguration().addMapper(mapperClass);
    }
    
    /**
     * 执行 Mapper 操作（自动管理 Session）
     * 
     * @param mapperClass Mapper 接口类
     * @param action 操作函数
     * @return 操作结果
     */
    public <T, R> R execute(Class<T> mapperClass, Function<T, R> action) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            T mapper = session.getMapper(mapperClass);
            return action.apply(mapper);
        }
    }
    
    /**
     * 执行事务操作（手动提交）
     * 
     * @param action 事务操作函数
     * @return 操作结果
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
}
