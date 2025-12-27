package litejava.plugins.database;

import litejava.Plugin;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 * 标准 JPA 插件 - 使用 persistence.xml 配置
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>javax.persistence</groupId>
 *     <artifactId>javax.persistence-api</artifactId>
 *     <version>2.2</version>
 * </dependency>
 * <!-- JPA 实现 (Hibernate/EclipseLink/OpenJPA) -->
 * <dependency>
 *     <groupId>org.hibernate</groupId>
 *     <artifactId>hibernate-core</artifactId>
 *     <version>5.6.15.Final</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置 persistence.xml</h2>
 * <pre>{@code
 * <!-- src/main/resources/META-INF/persistence.xml -->
 * <?xml version="1.0" encoding="UTF-8"?>
 * <persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence" version="2.2">
 *     <persistence-unit name="default">
 *         <class>com.example.entity.User</class>
 *         <class>com.example.entity.Order</class>
 *         <properties>
 *             <property name="javax.persistence.jdbc.url" value="jdbc:mysql://localhost:3306/mydb"/>
 *             <property name="javax.persistence.jdbc.user" value="root"/>
 *             <property name="javax.persistence.jdbc.password" value="root"/>
 *             <property name="javax.persistence.jdbc.driver" value="com.mysql.cj.jdbc.Driver"/>
 *             <property name="hibernate.dialect" value="org.hibernate.dialect.MySQL8Dialect"/>
 *             <property name="hibernate.show_sql" value="true"/>
 *             <property name="hibernate.hbm2ddl.auto" value="update"/>
 *         </properties>
 *     </persistence-unit>
 * </persistence>
 * }</pre>
 * 
 * <h2>应用配置覆盖 (可选)</h2>
 * <pre>{@code
 * jpa.url=jdbc:mysql://localhost:3306/mydb
 * jpa.username=root
 * jpa.password=root
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用默认 persistence-unit
 * JpaPlugin jpa = new JpaPlugin();
 * app.use(jpa);
 * 
 * // 指定 persistence-unit
 * JpaPlugin jpa = new JpaPlugin("myunit");
 * app.use(jpa);
 * 
 * // 使用 EntityManager
 * app.get("/users/:id", ctx -> {
 *     EntityManager em = jpa.em();
 *     try {
 *         User user = em.find(User.class, ctx.pathParam("id", Long.class));
 *         ctx.json(user);
 *     } finally {
 *         em.close();
 *     }
 * });
 * }</pre>
 * 
 * <h2>vs HibernatePlugin</h2>
 * <ul>
 *   <li>JpaPlugin - 标准 JPA，需要 persistence.xml，可切换实现</li>
 *   <li>HibernatePlugin - Hibernate 专用，编程式配置，支持包扫描</li>
 * </ul>
 */
public class JpaPlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static JpaPlugin instance;
    
    /** JPA EntityManagerFactory，初始化后可用 */
    public EntityManagerFactory emf;
    
    /** persistence.xml 中定义的 persistence-unit 名称 */
    public String persistenceUnit = "default";
    
    public JpaPlugin() {
        instance = this;
    }
    
    public JpaPlugin(String persistenceUnit) {
        instance = this;
        this.persistenceUnit = persistenceUnit;
    }
    
    @Override
    public void config() {
        Map<String, String> props = new HashMap<>();
        
        // 允许通过应用配置覆盖 persistence.xml 中的值
        String url = app.conf.getString("jpa", "url", null);
        if (url != null) {
            props.put("javax.persistence.jdbc.url", url);
        }
        String username = app.conf.getString("jpa", "username", null);
        if (username != null) {
            props.put("javax.persistence.jdbc.user", username);
        }
        String password = app.conf.getString("jpa", "password", null);
        if (password != null) {
            props.put("javax.persistence.jdbc.password", password);
        }
        String driver = app.conf.getString("jpa", "driver", null);
        if (driver != null) {
            props.put("javax.persistence.jdbc.driver", driver);
        }
        
        emf = Persistence.createEntityManagerFactory(persistenceUnit, props);
        app.log.info("JpaPlugin configured (persistence-unit: " + persistenceUnit + ")");
    }
    
    @Override
    public void uninstall() {
        if (emf != null) {
            emf.close();
        }
    }
    
    /**
     * 获取 EntityManager (用户自己管理生命周期)
     */
    public EntityManager em() {
        return emf.createEntityManager();
    }
    
    /**
     * 执行查询操作（自动管理 EntityManager 生命周期）
     */
    public <T> T query(java.util.function.Function<EntityManager, T> action) {
        EntityManager em = emf.createEntityManager();
        try {
            return action.apply(em);
        } finally {
            em.close();
        }
    }
    
    /**
     * 执行事务操作（自动管理 EntityManager 和事务）
     */
    public <T> T tx(java.util.function.Function<EntityManager, T> action) {
        EntityManager em = emf.createEntityManager();
        javax.persistence.EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = action.apply(em);
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            em.close();
        }
    }
    
    /**
     * 执行事务操作（无返回值）
     */
    public void txVoid(java.util.function.Consumer<EntityManager> action) {
        tx(em -> { action.accept(em); return null; });
    }
}
