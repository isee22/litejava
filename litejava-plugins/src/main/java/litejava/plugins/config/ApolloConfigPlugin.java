package litejava.plugins.config;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import litejava.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apollo 配置中心插件
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * apollo:
 *   appId: my-app
 *   meta: http://localhost:8080
 *   namespace: application
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new ApolloConfigPlugin());
 * 
 * // 获取配置
 * String value = ApolloConfigPlugin.instance.get("key");
 * String value = ApolloConfigPlugin.instance.get("key", "default");
 * 
 * // 监听配置变化
 * ApolloConfigPlugin.instance.onChange("key", (oldVal, newVal) -> {
 *     System.out.println("Config changed: " + oldVal + " -> " + newVal);
 * });
 * }</pre>
 */
public class ApolloConfigPlugin extends Plugin {
    
    public static ApolloConfigPlugin instance;
    
    /** Apollo 配置实例 */
    public Config config;
    
    /** App ID */
    public String appId;
    
    /** Meta Server 地址 */
    public String meta = "http://localhost:8080";
    
    /** 命名空间 */
    public String namespace = "application";
    
    /** 配置变更监听器 */
    private Map<String, ConfigChangeListener> listeners = new ConcurrentHashMap<>();
    
    @FunctionalInterface
    public interface ConfigChangeListener {
        void onChange(String oldValue, String newValue);
    }
    
    @Override
    public void config() {
        instance = this;
        
        appId = app.conf.getString("apollo", "appId", appId);
        meta = app.conf.getString("apollo", "meta", meta);
        namespace = app.conf.getString("apollo", "namespace", namespace);
        
        if (appId != null) {
            System.setProperty("app.id", appId);
        }
        System.setProperty("apollo.meta", meta);
        
        config = ConfigService.getConfig(namespace);
        
        config.addChangeListener(changeEvent -> {
            for (String key : changeEvent.changedKeys()) {
                ConfigChangeListener listener = listeners.get(key);
                if (listener != null) {
                    String oldValue = changeEvent.getChange(key).getOldValue();
                    String newValue = changeEvent.getChange(key).getNewValue();
                    listener.onChange(oldValue, newValue);
                }
            }
        });
        
        app.log.info("ApolloConfigPlugin: Connected to " + meta + ", namespace=" + namespace);
    }
    
    /**
     * 获取配置值
     */
    public String get(String key) {
        return config.getProperty(key, null);
    }
    
    /**
     * 获取配置值（带默认值）
     */
    public String get(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
    
    /**
     * 获取整数配置
     */
    public int getInt(String key, int defaultValue) {
        return config.getIntProperty(key, defaultValue);
    }
    
    /**
     * 获取长整数配置
     */
    public long getLong(String key, long defaultValue) {
        return config.getLongProperty(key, defaultValue);
    }
    
    /**
     * 获取布尔配置
     */
    public boolean getBool(String key, boolean defaultValue) {
        return config.getBooleanProperty(key, defaultValue);
    }
    
    /**
     * 获取所有配置键
     */
    public Set<String> getKeys() {
        return config.getPropertyNames();
    }
    
    /**
     * 监听配置变化
     */
    public void onChange(String key, ConfigChangeListener listener) {
        listeners.put(key, listener);
    }
    
    @Override
    public void uninstall() {
        instance = null;
        listeners.clear();
    }
}
