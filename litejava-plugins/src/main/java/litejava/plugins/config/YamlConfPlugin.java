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
 * <h2>配置优先级（从低到高）</h2>
 * <ol>
 *   <li>配置文件 (application.yml)</li>
 *   <li>环境配置 (application-{env}.yml)</li>
 *   <li>环境变量 (SERVER_PORT → server.port)</li>
 *   <li>命令行参数 (--server.port=9000)</li>
 * </ol>
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
 * // 带命令行参数
 * app.use(new YamlConfPlugin().args(args));
 * 
 * // 指定配置文件和环境
 * app.use(new YamlConfPlugin("config/app.yml", "dev").args(args));
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
    public YamlConfPlugin args(String[] args) {
        this.cmdArgs = args;
        return this;
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
        
        // 加载环境变量和命令行参数（继承自 ConfPlugin）
        loadEnvVars();
        loadCmdArgs();
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String filename) {
        try {
            // 先尝试从 classpath 加载
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
            if (is != null) {
                Map<String, Object> result = new Yaml().load(is);
                return result != null ? result : Collections.emptyMap();
            }
            // 再尝试从文件系统加载
            Map<String, Object> result = new Yaml().load(new FileInputStream(filename));
            return result != null ? result : Collections.emptyMap();
        } catch (FileNotFoundException e) {
            return Collections.emptyMap();
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
