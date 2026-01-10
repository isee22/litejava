package litejava.plugins.config;

import litejava.plugin.ConfPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;

/**
 * YAML 配置插件 - 从 application.yml 加载配置
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.yaml</groupId>
 *     <artifactId>snakeyaml</artifactId>
 *     <version>2.2</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 默认加载 application.yml
 * app.use(new YamlConfPlugin());
 * 
 * // 指定配置文件
 * app.use(new YamlConfPlugin("config/app.yml"));
 * 
 * // 读取配置
 * int port = app.conf.getInt("server", "port", 8080);
 * String dbUrl = app.conf.getString("database", "url", "");
 * }</pre>
 */
public class YamlConfPlugin extends ConfPlugin {
    
    private String file = "application.yml";
    private String env;
    
    public YamlConfPlugin() {}
    
    public YamlConfPlugin(String file) {
        this.file = file;
    }
    
    /**
     * 多环境配置
     * @param env 环境名 (dev/test/prod)
     * 会加载 application.yml 和 application-{env}.yml
     */
    public YamlConfPlugin(String file, String env) {
        this.file = file;
        this.env = env;
    }
    
    @Override
    public void config() {
        // 加载主配置
        data = loadYaml(file);
        
        // 加载环境配置并合并
        if (env != null && !env.isEmpty()) {
            String envFile = file.replace(".yml", "-" + env + ".yml");
            Map<String, Object> envData = loadYaml(envFile);
            merge(data, envData);
            app.env = env;
        }
        
        // 不调用 super.config()，因为我们已经加载了 YAML 配置
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String filename) {
        try {
            // 先尝试从文件系统加载（优先级更高，方便集群部署）
            java.io.File file = new java.io.File(filename);
            if (file.exists()) {
                Map<String, Object> result = new Yaml().load(new FileInputStream(file));
                return result != null ? result : Collections.emptyMap();
            }
            // 再尝试从 classpath 加载
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
            if (is != null) {
                Map<String, Object> result = new Yaml().load(is);
                return result != null ? result : Collections.emptyMap();
            }
            return Collections.emptyMap();
        } catch (FileNotFoundException e) {
            return Collections.emptyMap();
        }
    }
    
    /**
     * 从 YAML 字符串加载配置并合并到当前配置
     * @param yamlContent YAML 内容
     */
    @SuppressWarnings("unchecked")
    public void loadYamlContent(String yamlContent) {
        if (yamlContent == null || yamlContent.isEmpty()) return;
        
        Map<String, Object> newData = new Yaml().load(yamlContent);
        if (newData != null) {
            merge(data, newData);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void merge(Map<String, Object> base, Map<String, Object> override) {
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && base.get(key) instanceof Map) {
                merge((Map<String, Object>) base.get(key), (Map<String, Object>) value);
            } else {
                base.put(key, value);
            }
        }
    }
}
