package litejava.plugins.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存缓存插件 - 基于 ConcurrentHashMap 实现，直接存储对象
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 开发/测试环境使用
 * app.use(new MemoryCachePlugin());
 * 
 * // 存储对象
 * CachePlugin.instance.set("book:1", book);
 * Book book = CachePlugin.instance.get("book:1");
 * }</pre>
 * 
 * <h2>注意</h2>
 * <ul>
 *   <li>仅适用于单机开发/测试环境</li>
 *   <li>生产环境请使用 RedisCachePlugin</li>
 *   <li>不支持分布式，重启后数据丢失</li>
 * </ul>
 */
public class MemoryCachePlugin extends CachePlugin {
    
    /** 默认实例（单例访问） */
    public static MemoryCachePlugin instance;
    
    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();
    
    @Override
    public void config() {
        super.config();
        if (instance == null) instance = this;
    }
    
    @Override
    public void uninstall() {
        store.clear();
    }
    
    @Override
    public void set(String key, Object value, int ttlSeconds) {
        long expireAt = ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000L : 0;
        store.put(key(key), new CacheEntry(value, expireAt));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = store.get(key(key));
        if (entry == null) return null;
        if (entry.isExpired()) {
            store.remove(key(key));
            return null;
        }
        return (T) entry.value;
    }
    
    @Override
    public void del(String key) {
        String k = key(key);
        if (k.endsWith("*")) {
            String prefix = k.substring(0, k.length() - 1);
            store.keySet().removeIf(x -> x.startsWith(prefix));
        } else {
            store.remove(k);
        }
    }
    
    @Override
    public boolean exists(String key) {
        CacheEntry entry = store.get(key(key));
        if (entry == null) return false;
        if (entry.isExpired()) {
            store.remove(key(key));
            return false;
        }
        return true;
    }
    
    /** 清空所有缓存 */
    public void clear() {
        store.clear();
    }
    
    /** 获取缓存数量 */
    public int size() {
        return store.size();
    }
    
    private static class CacheEntry {
        final Object value;
        final long expireAt;
        
        CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
        
        boolean isExpired() {
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        }
    }
}
