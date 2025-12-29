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
 *   <li>支持多层嵌套格式（Spring Boot 风格）</li>
 *   <li>自动类型转换（数字、布尔值）</li>
 *   <li>默认加载 application.properties</li>
 *   <li>支持环境变量和命令行参数覆盖</li>
 * </ul>
 * 
 * <h2>配置优先级（从低到高）</h2>
 * <ol>
 *   <li>配置文件 (application.properties)</li>
 *   <li>环境变量 (SERVER_PORT → server.port)</li>
 *   <li>命令行参数 (--server.port=9000)</li>
 * </ol>
 * 
 * <h2>配置文件格式（Spring Boot 风格）</h2>
 * <pre>
 * # application.properties
 * server.port=8080
 * server.host=0.0.0.0
 * 
 * # 多层嵌套
 * database.primary.url=jdbc:mysql://localhost:3306/test
 * database.primary.username=root
 * 
 * # 数组/列表
 * static.mappings[0].url=/static
 * static.mappings[0].path=classpath:static/
 * static.mappings[1].url=/uploads
 * static.mappings[1].path=file:/data/uploads/
 * 
 * # 简单 Map（key 作为路径）
 * static.locations./static=classpath:static/
 * static.locations./uploads=file:/data/uploads/
 * </pre>
 * 
 * <h2>环境变量</h2>
 * <p>环境变量自动转换：SERVER_PORT → server.port
 * <pre>
 * # Linux/Mac
 * export SERVER_PORT=9000
 * 
 * # Windows
 * set SERVER_PORT=9000
 * </pre>
 * 
 * <h2>命令行参数</h2>
 * <pre>
 * java -jar app.jar --server.port=9000 --database.url=jdbc:mysql://prod:3306/db
 * </pre>
 * 
 * <h2>使用方式</h2>
 * <pre>{@code
 * public static void main(String[] args) {
 *     App app = new App();
 *     app.use(new ConfPlugin().args(args));  // 传入命令行参数
 *     app.run();
 * }
 * }</pre>
 * 
 * <h2>YAML 支持</h2>
 * <p>如需 YAML 格式配置，使用 litejava-plugins 模块中的 YamlConfPlugin：
 * <pre>{@code
 * app.use(new YamlConfPlugin().args(args));
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see Plugin 插件基类
 */
public class ConfPlugin extends Plugin {
    
    /** 单例插件，同类型自动替换 */
    @Override
    public boolean singleton() {
        return true;
    }
    
    /** 配置文件路径 */
    public String file = "application.properties";
    
    /** 配置数据存储 */
    protected Map<String, Object> data = new LinkedHashMap<>();
    
    /** 命令行参数 */
    protected String[] cmdArgs;
    
    public ConfPlugin() {}
    
    /**
     * 指定配置文件
     * @param file 配置文件路径
     */
    public ConfPlugin(String file) {
        this.file = file;
    }
    
    /**
     * 设置命令行参数
     * @param args main 方法的 args 参数
     * @return this
     */
    public ConfPlugin args(String[] args) {
        this.cmdArgs = args;
        return this;
    }
    
    @Override
    public void config() {
        super.config();
        loadProperties(file);
        loadEnvVars();
        loadCmdArgs();
    }
    
    @Override
    public void uninstall() {
        data.clear();
    }
    
    /**
     * 加载 .properties 文件
     * <p>支持多层嵌套格式：
     * <ul>
     *   <li>section.key=value</li>
     *   <li>section.sub.key=value（多层嵌套）</li>
     *   <li>section.list[0]=value（数组）</li>
     * </ul>
     */
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
                
                setNestedValue(data, key, parseValue(value));
            }
        } catch (IOException e) {
            System.err.println("Failed to load properties: " + e.getMessage());
        }
    }
    
    /**
     * 设置嵌套值，支持 a.b.c 和 a[0] 格式
     */
    @SuppressWarnings("unchecked")
    protected void setNestedValue(Map<String, Object> map, String key, Object value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = map;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            
            // 处理数组索引 key[0]
            int bracketIdx = part.indexOf('[');
            if (bracketIdx > 0) {
                String arrayKey = part.substring(0, bracketIdx);
                int index = Integer.parseInt(part.substring(bracketIdx + 1, part.length() - 1));
                
                List<Object> list = (List<Object>) current.get(arrayKey);
                if (list == null) {
                    list = new ArrayList<>();
                    current.put(arrayKey, list);
                }
                
                // 扩展列表大小
                while (list.size() <= index) {
                    list.add(new LinkedHashMap<String, Object>());
                }
                
                current = (Map<String, Object>) list.get(index);
            } else {
                Object next = current.get(part);
                if (!(next instanceof Map)) {
                    next = new LinkedHashMap<String, Object>();
                    current.put(part, next);
                }
                current = (Map<String, Object>) next;
            }
        }
        
        // 设置最终值
        String lastPart = parts[parts.length - 1];
        int bracketIdx = lastPart.indexOf('[');
        if (bracketIdx > 0) {
            String arrayKey = lastPart.substring(0, bracketIdx);
            int index = Integer.parseInt(lastPart.substring(bracketIdx + 1, lastPart.length() - 1));
            
            List<Object> list = (List<Object>) current.get(arrayKey);
            if (list == null) {
                list = new ArrayList<>();
                current.put(arrayKey, list);
            }
            
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, value);
        } else {
            current.put(lastPart, value);
        }
    }
    
    /**
     * 解析值类型：数字、布尔、字符串
     */
    protected Object parseValue(String value) {
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
     * 加载环境变量
     * <p>环境变量名转换规则：SERVER_PORT → server.port
     */
    protected void loadEnvVars() {
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 转换环境变量名：SERVER_PORT → server.port
            String configKey = key.toLowerCase().replace('_', '.');
            
            if (configKey.contains(".")) {
                setNestedValue(data, configKey, parseValue(value));
            }
        }
    }
    
    /**
     * 加载命令行参数
     * <p>格式：--server.port=9000
     */
    protected void loadCmdArgs() {
        if (cmdArgs == null) return;
        
        for (String arg : cmdArgs) {
            if (!arg.startsWith("--")) continue;
            
            int eq = arg.indexOf('=');
            if (eq <= 2) continue;
            
            String key = arg.substring(2, eq);
            String value = arg.substring(eq + 1);
            
            setNestedValue(data, key, parseValue(value));
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
     * 获取指定路径的配置
     * <p>支持点号路径：get("static.mappings") 等价于 get("static").get("mappings")
     * @param path 配置路径，如 "server" 或 "static.mappings"
     * @return 配置 Map 或空 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String path) {
        Object val = getByPath(path);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return Collections.emptyMap();
    }
    
    /**
     * 根据点号路径获取值
     * @param path 配置路径，如 "server.port" 或 "static.mappings"
     * @return 配置值或 null
     */
    @SuppressWarnings("unchecked")
    protected Object getByPath(String path) {
        String[] parts = path.split("\\.");
        Object current = data;
        
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
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
    
    /**
     * 获取文件大小配置（支持 Spring Boot 风格单位）
     * <p>支持格式：
     * <ul>
     *   <li>纯数字：字节数，如 5242880</li>
     *   <li>KB/K：千字节，如 512KB</li>
     *   <li>MB/M：兆字节，如 5MB</li>
     *   <li>GB/G：吉字节，如 1GB</li>
     * </ul>
     * 
     * @param section section 名称
     * @param key 配置键
     * @param defaultValue 默认值（字节）
     * @return 字节数
     */
    public long getSize(String section, String key, long defaultValue) {
        Object val = get(section).get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).longValue();
        return parseSize(String.valueOf(val), defaultValue);
    }
    
    /**
     * 解析文件大小字符串
     * @param value 大小字符串，如 "5MB", "1GB", "512KB"
     * @param defaultValue 解析失败时的默认值
     * @return 字节数
     */
    public static long parseSize(String value, long defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        
        value = value.trim().toUpperCase();
        
        try {
            if (value.endsWith("GB") || value.endsWith("G")) {
                String num = value.endsWith("GB") ? value.substring(0, value.length() - 2) : value.substring(0, value.length() - 1);
                return (long) (Double.parseDouble(num.trim()) * 1024 * 1024 * 1024);
            }
            if (value.endsWith("MB") || value.endsWith("M")) {
                String num = value.endsWith("MB") ? value.substring(0, value.length() - 2) : value.substring(0, value.length() - 1);
                return (long) (Double.parseDouble(num.trim()) * 1024 * 1024);
            }
            if (value.endsWith("KB") || value.endsWith("K")) {
                String num = value.endsWith("KB") ? value.substring(0, value.length() - 2) : value.substring(0, value.length() - 1);
                return (long) (Double.parseDouble(num.trim()) * 1024);
            }
            if (value.endsWith("B")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim());
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
