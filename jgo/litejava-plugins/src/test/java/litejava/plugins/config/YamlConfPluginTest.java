package litejava.plugins.config;

import litejava.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * YamlConfPlugin 测试
 */
class YamlConfPluginTest {
    
    static File tempConfigFile;
    
    @BeforeAll
    static void setup() throws Exception {
        // 创建临时配置文件
        tempConfigFile = File.createTempFile("test-config", ".yml");
        tempConfigFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempConfigFile)) {
            writer.write("server:\n");
            writer.write("  port: 9090\n");
            writer.write("  devMode: false\n");
            writer.write("\n");
            writer.write("database:\n");
            writer.write("  url: jdbc:mysql://localhost:3306/test\n");
            writer.write("  username: root\n");
            writer.write("  password: secret\n");
            writer.write("  pool:\n");
            writer.write("    maxSize: 20\n");
        }
    }
    
    @Test
    void testLoadConfig() {
        App app = new App();
        YamlConfPlugin conf = new YamlConfPlugin(tempConfigFile.getAbsolutePath());
        app.use(conf);
        
        // 验证 server 配置已加载
        assertEquals(9090, app.conf.getInt("server", "port", 8080));
        assertFalse(app.conf.getBool("server", "devMode", true));
    }
    
    @Test
    void testGetString() {
        App app = new App();
        YamlConfPlugin conf = new YamlConfPlugin(tempConfigFile.getAbsolutePath());
        app.use(conf);
        
        assertEquals("jdbc:mysql://localhost:3306/test", app.conf.getString("database", "url", ""));
        assertEquals("root", app.conf.getString("database", "username", ""));
        assertEquals("secret", app.conf.getString("database", "password", ""));
    }
    
    @Test
    void testGetInt() {
        App app = new App();
        YamlConfPlugin conf = new YamlConfPlugin(tempConfigFile.getAbsolutePath());
        app.use(conf);
        
        // YAML 嵌套结构: database.pool.maxSize
        // 需要先获取 database，再获取 pool
        @SuppressWarnings("unchecked")
        Map<String, Object> pool = (Map<String, Object>) app.conf.get("database").get("pool");
        assertEquals(20, ((Number) pool.get("maxSize")).intValue());
    }
    
    @Test
    void testDefaultValue() {
        App app = new App();
        YamlConfPlugin conf = new YamlConfPlugin(tempConfigFile.getAbsolutePath());
        app.use(conf);
        
        // 不存在的配置返回默认值
        assertEquals("default", app.conf.getString("nonexistent", "key", "default"));
        assertEquals(100, app.conf.getInt("nonexistent", "key", 100));
    }
    
    @Test
    void testMissingFile() {
        App app = new App();
        YamlConfPlugin conf = new YamlConfPlugin("nonexistent-file.yml");
        
        // 不应该抛异常，只是配置为空
        assertDoesNotThrow(() -> app.use(conf));
    }
    
    @Test
    void testEnvConfig() throws Exception {
        // 创建环境配置文件
        File envFile = new File(tempConfigFile.getParent(), 
            tempConfigFile.getName().replace(".yml", "-prod.yml"));
        envFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(envFile)) {
            writer.write("server:\n");
            writer.write("  port: 80\n");
        }
        
        App app = new App();
        YamlConfPlugin conf = new YamlConfPlugin(tempConfigFile.getAbsolutePath(), "prod");
        app.use(conf);
        
        // 环境配置覆盖主配置
        assertEquals(80, app.conf.getInt("server", "port", 8080));
        assertEquals("prod", app.env);
    }
}
