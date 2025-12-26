package litejava.plugins.vt;

import com.zaxxer.hikari.HikariConfig;
import litejava.plugins.database.JdbcPlugin;

/**
 * JDBC 插件 - 虚拟线程版本 (Java 21+)
 * 继承 JdbcPlugin，使用虚拟线程工厂配置 HikariCP
 */
public class JdbcVirtualThreadPlugin extends JdbcPlugin {
    
    public JdbcVirtualThreadPlugin() {
        super();
    }
    
    public JdbcVirtualThreadPlugin(String configPrefix) {
        super(configPrefix);
    }
    
    @Override
    protected HikariConfig createHikariConfig() {
        HikariConfig config = super.createHikariConfig();
        config.setThreadFactory(Thread.ofVirtual().factory());
        return config;
    }
}
