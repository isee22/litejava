package litejava.plugin;

import java.io.*;
import java.util.*;

import litejava.Plugin;

/**
 * 配置插件 - 支持 .properties 文件（零外部依赖）
 * 
 * <p>这是 LiteJava 的默认配置插件，特点：
 * <ul>
 *   <li>零外部依赖，纯 JDK 实现</li>
 *   <li>支持 section.key 格式</li>
 *   <li>自动类型转换（数字、布尔值）</li>
 *   <li>默认加载 application.properties</li>
 * </ul>
 * 
 * <h2>配置文件格式</h2>
 * <pre>
 * # application.properties
 * 
 * # 服务器配置
 * server.port=8080
 * server.host=0.0.0.0
 * server.devMode=true
 * server.charset=UTF-8
 * 
 * # 日志配置
 * log.level=INFO
 * log.file=logs/app.log
 * 
 * # 数据库配置
 * database.url=jdbc:mysql://localhost:3306/test
 * database.username=root
 * database.password=123456
 * database.poolSize=10
 * 
 * # Redis 配置
 * redis.host=localhost
 * redis.port=6379
 * </pre>
 * 
 * <h2>使用方式</h2>
 * <pre>{@code
 * // 方式一：默认加载 application.properties
 * app.use(new ConfPlugin());
 * 
 * // 方式二：指定配置文件
 * app.use(new ConfPlugin("config.properties"));
 * 
 * // 读取配置
 * int port = app.conf.getInt("server", "port", 8080);
 * String host = app.conf.getString("redis", "host", "localhost");
 * boolean devMode = app.conf.getBool("server", "devMode", true);
 * }</pre>
 * 
 * <h2>YAML 支持</h2>
 * <p>如需 YAML 格式配置，使用 litejava-plugins 模块中的 YamlConfPlugin：
 * <pre>{@code
 * app.use(new YamlConfPlugin());  // 加载 application.yml
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see Plugin 插件基类
 */
public class ConfPlugin extends Plugin {
    
    /** 配置文件路径 */
    public String file = "application.properties";
    
    /** 配置数据存储 */
    protected Map<String, Object> data = new LinkedHashMap<>();
    
    public ConfPlugin() {}
    
    /**
     * 指定配置文件
     * @param file 配置文件路径
     */
    public ConfPlugin(String file) {
        this.file = file;
    }
    
    @Override
    public void config() {
        super.config();
        loadProperties(file);
    }
    
    @Override
    public void uninstall() {
        data.clear();
    }
    
    /**
     * 加载 .properties 文件
     * <p>支持 section.key 格式，自动解析为嵌套 Map
     */
    @SuppressWarnings("unchecked")
    protected void loadProperties(String filename) {
        File f = new File(filename);
        if (!f.exists()) return;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
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
                    
                    Map<String, Object> sectionMap = (Map<String, Object>) data.get(section);
                    if (sectionMap == null) {
                        sectionMap = new LinkedHashMap<>();
                        data.put(section, sectionMap);
                    }
                    sectionMap.put(subKey, parseValue(value));
                } else {
                    data.put(key, parseValue(value));
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }
    
    /**
     * 解析值类型：数字、布尔、字符串
     */
    private Object parseValue(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
    
    /**
     * 获取所有配置
     * @return 配置 Map
     */
    public Map<String, Object> get() {
        return data;
    }
    
    /**
     * 获取指定 section 的配置
     * @param section section 名称
     * @return section 配置 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String section) {
        Object val = data.get(section);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return Collections.emptyMap();
    }
    
    /**
     * 获取字符串配置
     * @param section section 名称
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String section, String key, String defaultValue) {
        Object val = get(section).get(key);
        return val != null ? String.valueOf(val) : defaultValue;
    }
    
    /**
     * 获取整数配置
     * @param section section 名称
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getInt(String section, String key, int defaultValue) {
        Object val = get(section).get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) {}
        }
        return defaultValue;
    }
    
    /**
     * 获取长整数配置
     * @param section section 名称
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public long getLong(String section, String key, long defaultValue) {
        Object val = get(section).get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) {}
        }
        return defaultValue;
    }
    
    /**
     * 获取布尔配置
     * @param section section 名称
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBool(String section, String key, boolean defaultValue) {
        Object val = get(section).get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(String.valueOf(val));
        return defaultValue;
    }
}
