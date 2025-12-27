package litejava.plugins.database;

import litejava.Plugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Hibernate 插件 - 编程式配置，无需 persistence.xml
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.hibernate</groupId>
 *     <artifactId>hibernate-core</artifactId>
 *     <version>5.6.15.Final</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * hibernate.url=jdbc:mysql://localhost:3306/mydb
 * hibernate.username=root
 * hibernate.password=root
 * hibernate.driver=com.mysql.cj.jdbc.Driver
 * hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
 * hibernate.showSql=true
 * hibernate.hbm2ddl=update
 * hibernate.packages=com.example.entity    # 实体扫描包 (逗号分隔)
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式1: 配置文件指定扫描包
 * HibernatePlugin hb = new HibernatePlugin();
 * app.use(hb);
 * 
 * // 方式2: 代码指定扫描包
 * HibernatePlugin hb = new HibernatePlugin();
 * hb.packages.add("com.example.entity");
 * app.use(hb);
 * 
 * // 方式3: 手动注册实体类
 * HibernatePlugin hb = new HibernatePlugin();
 * hb.entities.add(User.class);
 * hb.entities.add(Order.class);
 * app.use(hb);
 * 
 * // 使用 EntityManager (JPA 标准接口)
 * app.get("/users/:id", ctx -> {
 *     EntityManager em = hb.em();
 *     try {
 *         User user = em.find(User.class, ctx.pathParam("id", Long.class));
 *         ctx.json(user);
 *     } finally {
 *         em.close();
 *     }
 * });
 * 
 * // 或使用 Hibernate Session (原生接口)
 * app.get("/users/:id", ctx -> {
 *     try (Session session = hb.sessionFactory.openSession()) {
 *         User user = session.get(User.class, ctx.pathParam("id", Long.class));
 *         ctx.json(user);
 *     }
 * });
 * }</pre>
 * 
 * <h2>vs JpaPlugin</h2>
 * <ul>
 *   <li>JpaPlugin - 标准 JPA，需要 persistence.xml，可切换实现</li>
 *   <li>HibernatePlugin - Hibernate 专用，编程式配置，支持包扫描</li>
 * </ul>
 */
public class HibernatePlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static HibernatePlugin instance;
    
    /** Hibernate SessionFactory，初始化后可用 */
    public SessionFactory sessionFactory;
    
    /** 要扫描 @Entity 的包名列表，可在 use() 前添加 */
    public List<String> packages = new ArrayList<>();
    
    /** 手动注册的实体类列表，可在 use() 前添加 */
    public List<Class<?>> entities = new ArrayList<>();
    
    public HibernatePlugin() {
        instance = this;
    }
    
    @Override
    public void config() {
        Configuration cfg = new Configuration();
        
        // 数据库配置
        String url = app.conf.getString("hibernate", "url", null);
        if (url != null) {
            cfg.setProperty("hibernate.connection.url", url);
            cfg.setProperty("hibernate.connection.username", app.conf.getString("hibernate", "username", ""));
            cfg.setProperty("hibernate.connection.password", app.conf.getString("hibernate", "password", ""));
            cfg.setProperty("hibernate.connection.driver_class", app.conf.getString("hibernate", "driver", ""));
        }
        
        String dialect = app.conf.getString("hibernate", "dialect", null);
        if (dialect != null) {
            cfg.setProperty("hibernate.dialect", dialect);
        }
        
        cfg.setProperty("hibernate.show_sql", app.conf.getString("hibernate", "showSql", "false"));
        cfg.setProperty("hibernate.hbm2ddl.auto", app.conf.getString("hibernate", "hbm2ddl", "none"));
        
        // 从配置文件读取扫描包
        String configPackages = app.conf.getString("hibernate", "packages", null);
        if (configPackages != null) {
            for (String pkg : configPackages.split(",")) {
                packages.add(pkg.trim());
            }
        }
        
        // 扫描包中的 @Entity 类
        for (String pkg : packages) {
            scanEntities(pkg, cfg);
        }
        
        // 注册手动添加的实体类
        for (Class<?> entity : entities) {
            cfg.addAnnotatedClass(entity);
        }
        
        sessionFactory = cfg.buildSessionFactory();
        app.log.info("HibernatePlugin configured");
    }
    
    @Override
    public void uninstall() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
    
    /**
     * 获取 EntityManager (JPA 标准接口)
     */
    public EntityManager em() {
        return sessionFactory.createEntityManager();
    }
    
    private void scanEntities(String packageName, Configuration cfg) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> urls = cl.getResources(path);
            
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                File dir = new File(url.toURI());
                if (dir.exists()) {
                    scanDirectory(dir, packageName, cfg);
                }
            }
        } catch (Exception e) {
            app.log.warn("Failed to scan package: " + packageName);
        }
    }
    
    private void scanDirectory(File dir, String packageName, Configuration cfg) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), cfg);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Entity.class)) {
                    cfg.addAnnotatedClass(clazz);
                    app.log.info("HibernatePlugin: Found entity " + clazz.getSimpleName());
                }
            }
        }
    }
}
