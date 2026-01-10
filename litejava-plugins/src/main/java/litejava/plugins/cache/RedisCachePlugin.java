package litejava.plugins.cache;

import litejava.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Redis 缓存插件
 * 
 * 配置示例：
 * redis:
 *   host: localhost
 *   port: 6379
 *   password: 
 *   database: 0
 *   maxTotal: 100
 *   maxIdle: 20
 *   minIdle: 5
 *   timeout: 3000
 * 
 * 使用示例：
 * <pre>{@code
 * RedisCachePlugin cache = app.getPlugin(RedisCachePlugin.class);
 * 
 * // 基本操作
 * cache.set("user:1", userJson, 3600);
 * String user = cache.get("user:1");
 * 
 * // 缓存穿透保护
 * User user = cache.getOrLoad("user:" + id, 3600, () -> userService.findById(id));
 * 
 * // Hash 操作
 * cache.hset("product:1", "stock", "100");
 * String stock = cache.hget("product:1", "stock");
 * }</pre>
 */
public class RedisCachePlugin extends Plugin {
    
    public String host = "localhost";
    public int port = 6379;
    public String password = null;
    public int database = 0;
    public int maxTotal = 100;
    public int maxIdle = 20;
    public int minIdle = 5;
    public int timeout = 3000;
    
    private JedisPool pool;
    
    @Override
    public void config() {
        host = app.conf.getString("redis", "host", host);
        port = app.conf.getInt("redis", "port", port);
        password = app.conf.getString("redis", "password", null);
        database = app.conf.getInt("redis", "database", database);
        maxTotal = app.conf.getInt("redis", "maxTotal", maxTotal);
        maxIdle = app.conf.getInt("redis", "maxIdle", maxIdle);
        minIdle = app.conf.getInt("redis", "minIdle", minIdle);
        timeout = app.conf.getInt("redis", "timeout", timeout);
        
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWait(Duration.ofMillis(timeout));
        config.setTestOnBorrow(true);
        
        if (password != null && !password.isEmpty()) {
            pool = new JedisPool(config, host, port, timeout, password, database);
        } else {
            pool = new JedisPool(config, host, port, timeout, null, database);
        }
        
        app.log.info("[RedisCache] 已连接: " + host + ":" + port + "/" + database);
    }
    
    // ==================== String 操作 ====================
    
    public void set(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(key, value);
        }
    }
    
    public void set(String key, String value, int expireSeconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, expireSeconds, value);
        }
    }
    
    public String get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
        }
    }
    
    public void del(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }
    
    public boolean exists(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.exists(key);
        }
    }
    
    public void expire(String key, int seconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.expire(key, seconds);
        }
    }
    
    // ==================== JSON 操作 ====================
    
    public void setJson(String key, Object value, int expireSeconds) {
        set(key, app.json.stringify(value), expireSeconds);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getJson(String key, Class<T> clazz) {
        String value = get(key);
        if (value == null) return null;
        if (clazz == Map.class) {
            return (T) app.json.parseMap(value);
        }
        return app.json.parse(value, clazz);
    }
    
    /**
     * 缓存穿透保护 - 先查缓存，没有则加载并缓存
     */
    public <T> T getOrLoad(String key, int expireSeconds, Supplier<T> loader) {
        String cached = get(key);
        if (cached != null) {
            return app.json.parse(cached, (Class<T>) Object.class);
        }
        
        T value = loader.get();
        if (value != null) {
            setJson(key, value, expireSeconds);
        }
        return value;
    }
    
    // ==================== Hash 操作 ====================
    
    public void hset(String key, String field, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, field, value);
        }
    }
    
    public String hget(String key, String field) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget(key, field);
        }
    }
    
    public Map<String, String> hgetAll(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hgetAll(key);
        }
    }
    
    public void hdel(String key, String... fields) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hdel(key, fields);
        }
    }
    
    public long hincrBy(String key, String field, long value) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hincrBy(key, field, value);
        }
    }
    
    // ==================== 计数器 ====================
    
    public long incr(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.incr(key);
        }
    }
    
    public long incrBy(String key, long value) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.incrBy(key, value);
        }
    }
    
    public long decr(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.decr(key);
        }
    }
    
    // ==================== 分布式锁 ====================
    
    /**
     * 尝试获取分布式锁
     * @param key 锁的 key
     * @param expireSeconds 锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String key, int expireSeconds) {
        try (Jedis jedis = pool.getResource()) {
            String result = jedis.set(key, "1", new redis.clients.jedis.params.SetParams().nx().ex(expireSeconds));
            return "OK".equals(result);
        }
    }
    
    /**
     * 释放分布式锁
     */
    public void unlock(String key) {
        del(key);
    }
    
    /**
     * 在分布式锁保护下执行操作
     * @param key 锁的 key
     * @param expireSeconds 锁过期时间（秒）
     * @param action 要执行的操作
     * @return 操作返回值
     */
    public <T> T withLock(String key, int expireSeconds, Supplier<T> action) {
        if (!tryLock(key, expireSeconds)) {
            throw new RuntimeException("获取锁失败: " + key);
        }
        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }
    
    /**
     * 在分布式锁保护下执行操作（无返回值）
     */
    public void withLock(String key, int expireSeconds, Runnable action) {
        if (!tryLock(key, expireSeconds)) {
            throw new RuntimeException("获取锁失败: " + key);
        }
        try {
            action.run();
        } finally {
            unlock(key);
        }
    }
    
    // ==================== 原生 Jedis ====================
    
    public Jedis getJedis() {
        return pool.getResource();
    }
    
    @Override
    public void uninstall() {
        if (pool != null) {
            pool.close();
        }
    }
}
