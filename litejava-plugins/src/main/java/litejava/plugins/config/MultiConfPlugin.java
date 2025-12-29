package litejava.plugins.config;

import litejava.plugin.ConfPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

/**
 * 多配置文件插件 - 支持同时加载多个 yml 和 properties 文件
 * 
 * <h2>特性</h2>
 * <ul>
 *   <li>支持 .yml 和 .properties 格式</li>
 *   <li>默认加载 application.yml 和 application.properties（如果存在）</li>
 *   <li>支持加载额外配置文件</li>
 *   <li>后加载的文件覆盖先加载的配置</li>
 *   <li>支持环境配置（自动加载 application-{env}.yml/properties）</li>
 *   <li>支持环境变量和命令行参数覆盖</li>
 * </ul>
 * 
 * <h2>配置优先级（从低到高）</h2>
 * <ol>
 *   <li>application.yml</li>
 *   <li>application.properties（覆盖 yml 同名配置）</li>
 *   <li>application-{env}.yml（如果设置了 env）</li>
 *   <li>application-{env}.properties（如果设置了 env）</li>
 *   <li>通过 load() 添加的额外文件</li>
 *   <li>环境变量 (SERVER_PORT → server.port)</li>
 *   <li>命令行参数 (--server.port=9000)</li>
 * </ol>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 最简用法
 * app.use(new MultiConfPlugin());
 * 
 * // 带命令行参数
 * app.use(new MultiConfPlugin().args(args));
 * 
 * // 带环境配置
 * app.use(new MultiConfPlugin().env("dev").args(args));
 * 
 * // 加载额外配置文件
 * app.use(new MultiConfPlugin()
 *     .load("database.properties")
 *     .load("redis.yml")
 *     .args(args));
 * }</pre>
 */
public class MultiConfPlugin extends ConfPlugin {
    
    private List<String> files = new ArrayList<>();
    private String env;
    
    public MultiConfPlugin() {}
    
    /**
     * 添加配置文件
     * @param file 配置文件路径（支持 .yml 和 .properties）
     * @return this
     */
    public MultiConfPlugin load(String file) {
        files.add(file);
        return this;
    }
    
    /**
     * 设置环境，自动加载 application-{env}.yml
     * @param env 环境名 (dev/test/prod)
     * @return this
     */
    public MultiConfPlugin env(String env) {
        this.env = env;
        return this;
    }
    
    @Override
    public MultiConfPlugin args(String[] args) {
        this.cmdArgs = args;
        return this;
    }
    
    @Override
    public void config() {
        // 不调用 super.config()，自己处理加载
        data = new LinkedHashMap<>();
        
        // 1. 默认加载 application.yml 和 application.properties
        loadFile("application.yml");
        loadFile("application.properties");
        
        // 2. 如果设置了 env，加载环境配置
        if (env != null) {
            loadFile("application-" + env + ".yml");
            loadFile("application-" + env + ".properties");
            app.env = env;
        }
        
        // 3. 加载额外配置文件
        for (String file : files) {
            loadFile(file);
        }
        
        // 4. 加载环境变量和命令行参数（继承自 ConfPlugin）
        loadEnvVars();
        loadCmdArgs();
    }
    
    private void loadFile(String filename) {
        if (filename.endsWith(".yml") || filename.endsWith(".yaml")) {
            Map<String, Object> yamlData = loadYaml(filename);
            merge(data, yamlData);
        } else if (filename.endsWith(".properties")) {
            Map<String, Object> propsData = loadProps(filename);
            merge(data, propsData);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String filename) {
        try {
            // 先尝试从 classpath 加载
            InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
            if (is != null) {
                Map<String, Object> result = new Yaml().load(is);
                return result != null ? result : Collections.emptyMap();
            }
            // 再尝试从文件系统加载
            File f = new File(filename);
            if (f.exists()) {
                Map<String, Object> result = new Yaml().load(new FileInputStream(f));
                return result != null ? result : Collections.emptyMap();
            }
        } catch (Exception e) {
            // Config file not found or parse error - use defaults
        }
        return Collections.emptyMap();
    }
    
    private Map<String, Object> loadProps(String filename) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            InputStream is = null;
            // 先尝试从 classpath 加载
            is = getClass().getClassLoader().getResourceAsStream(filename);
            if (is == null) {
                // 再尝试从文件系统加载
                File f = new File(filename);
                if (f.exists()) {
                    is = new FileInputStream(f);
                }
            }
            
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        
                        int eq = line.indexOf('=');
                        if (eq <= 0) continue;
                        
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        
                        // 解析 section.key 格式
                        int dot = key.indexOf('.');
                        if (dot > 0) {
                            String section = key.substring(0, dot);
                            String subKey = key.substring(dot + 1);
                            
                            Map<String, Object> sectionMap = (Map<String, Object>) result.get(section);
                            if (sectionMap == null) {
                                sectionMap = new LinkedHashMap<>();
                                result.put(section, sectionMap);
                            }
                            sectionMap.put(subKey, parseValue(value));
                        } else {
                            result.put(key, parseValue(value));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Config file not found or parse error - use defaults
        }
        return result;
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
