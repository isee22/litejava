package litejava.plugins.cache;

import net.spy.memcached.MemcachedClient;

import java.net.InetSocketAddress;

/**
 * Memcache 缓存插件 - 基于 spymemcached 实现
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>net.spy</groupId>
 *     <artifactId>spymemcached</artifactId>
 *     <version>2.12.3</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>{@code
 * memcache:
 *   host: localhost
 *   port: 11211
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new MemcacheCachePlugin());
 * 
 * // 获取缓存插件
 * CachePlugin cache = app.plugin(CachePlugin.class);
 * 
 * // 设置缓存
 * cache.set("user:1", "{\"name\":\"Tom\"}");
 * 
 * // 设置带过期时间 (秒)
 * cache.set("token:abc", "123456", 3600);
 * 
 * // 获取
 * String value = cache.get("user:1");
 * 
 * // 删除
 * cache.del("user:1");
 * 
 * // 直接使用 MemcachedClient (高级操作)
 * MemcacheCachePlugin mc = (MemcacheCachePlugin) app.plugin(CachePlugin.class);
 * mc.client.incr("counter", 1);
 * }</pre>
 */
public class MemcacheCachePlugin extends CachePlugin {
    
    public MemcachedClient client;
    public String host = "localhost";
    public int port = 11211;
    public int defaultTtl = 0;  // 0 = 永不过期
    
    @Override
    public void config() {
        host = app.conf.getString("memcache", "host", host);
        port = app.conf.getInt("memcache", "port", port);
        
        try {
            client = new MemcachedClient(new InetSocketAddress(host, port));
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Memcache", e);
        }
    }
    
    @Override
    public void uninstall() {
        if (client != null) client.shutdown();
    }
    
    @Override
    public void set(String key, Object value, int ttlSeconds) {
        String json = toJson(value);
        client.set(key(key), ttlSeconds > 0 ? ttlSeconds : defaultTtl, json);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = client.get(key(key));
        return value != null ? (T) fromJson(value.toString()) : null;
    }
    
    @Override
    public void del(String key) {
        client.delete(key(key));
    }
    
    @Override
    public boolean exists(String key) {
        return client.get(key(key)) != null;
    }
    
    // ==================== JSON 序列化 ====================
    
    private String toJson(Object obj) {
        if (obj instanceof String) return (String) obj;
        if (app != null && app.json != null) {
            return app.json.stringify(obj);
        }
        throw new IllegalStateException("No JsonPlugin configured for object serialization");
    }
    
    private Object fromJson(String json) {
        if (app != null && app.json != null) {
            return app.json.parseMap(json);
        }
        return json;
    }
}
