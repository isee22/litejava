package litejava.plugins.cache;

import litejava.Plugin;

import java.util.function.Supplier;

/**
 * 缓存插件基类 - 由具体实现提供缓存功能
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * cache:
 *   keyPrefix: "myapp:"    # 全局 key 前缀，避免多应用冲突
 *   defaultTtl: 3600       # 默认过期时间 (秒)
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 开发环境 - 内存缓存
 * app.use(new MemoryCachePlugin());
 * 
 * // 生产环境 - Redis
 * app.use(new RedisCachePlugin());
 * 
 * // 通过 instance 访问
 * CachePlugin.instance.set("book:1", book);
 * Book book = CachePlugin.instance.get("book:1");
 * CachePlugin.instance.del("book:*");  // 通配符删除
 * 
 * // 获取或加载
 * Book book = CachePlugin.instance.getOrLoad("book:1", () -> db.find(1));
 * }</pre>
 */
public abstract class CachePlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static CachePlugin instance;
    
    /** Key 前缀，避免多应用共用缓存时冲突 */
    public String keyPrefix = "";
    
    /** 默认过期时间 (秒)，0 表示永不过期 */
    public int defaultTtl = 3600;
    
    @Override
    public void config() {
        keyPrefix = app.conf.getString("cache", "keyPrefix", keyPrefix);
        defaultTtl = app.conf.getInt("cache", "defaultTtl", defaultTtl);
        if (instance == null) instance = this;
    }
    
    @Override
    public void uninstall() {}
    
    /** 带前缀的完整 key */
    public String key(String key) {
        return keyPrefix + key;
    }
    
    // ==================== 基础操作 (子类实现) ====================
    
    public void set(String key, Object value) {
        set(key, value, defaultTtl);
    }
    
    public abstract void set(String key, Object value, int ttlSeconds);
    
    public abstract <T> T get(String key);
    
    public abstract void del(String key);
    
    public abstract boolean exists(String key);
    
    // ==================== 获取或加载 ====================
    
    /** 获取或加载 */
    public <T> T getOrLoad(String key, Supplier<T> loader) {
        return getOrLoad(key, defaultTtl, loader);
    }
    
    /** 获取或加载 (指定过期时间) */
    public <T> T getOrLoad(String key, int ttlSeconds, Supplier<T> loader) {
        T value = get(key);
        if (value != null) {
            return value;
        }
        
        value = loader.get();
        if (value != null) {
            set(key, value, ttlSeconds);
        }
        return value;
    }
}
