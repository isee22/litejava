package litejava.plugins.cache;

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
public class RedisCachePlugin extends CachePlugin {
    
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
        
        // 重试连接 Redis，直到成功
        int retryCount = 0;
        while (true) {
            try {
                if (password != null && !password.isEmpty()) {
                    pool = new JedisPool(config, host, port, timeout, password, database);
                } else {
                    pool = new JedisPool(config, host, port, timeout, null, database);
                }
                
                // 测试连接
                try (Jedis jedis = pool.getResource()) {
                    jedis.ping();
                }
                
                app.log.info("[RedisCache] 已连接: " + host + ":" + port + "/" + database);
                break;  // 连接成功，退出循环
            } catch (Exception e) {
                retryCount++;
                int waitSeconds = Math.min(retryCount * 2, 30);  // 最多等待30秒
                app.log.warn("[RedisCache] 连接失败 (第" + retryCount + "次), " + waitSeconds + "秒后重试: " + e.getMessage());
                
                if (pool != null) {
                    try { pool.close(); } catch (Exception ignored) {}
                    pool = null;
                }
                
                try {
                    Thread.sleep(waitSeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Redis 连接被中断", ie);
                }
            }
        }
    }
    
    // ==================== CachePlugin 抽象方法实现 ====================
    
    @Override
    public void set(String key, Object value, int ttlSeconds) {
        String json = (value instanceof String) ? (String) value : app.json.stringify(value);
        try (Jedis jedis = pool.getResource()) {
            if (ttlSeconds > 0) {
                jedis.setex(key, ttlSeconds, json);
            } else {
                jedis.set(key, json);
            }
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return (T) jedis.get(key);
        }
    }
    
    @Override
    public void del(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }
    
    @Override
    public boolean exists(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.exists(key);
        }
    }
    
    // ==================== String 操作 (便捷方法) ====================
    
    public void setString(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(key, value);
        }
    }
    
    public void setString(String key, String value, int expireSeconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, expireSeconds, value);
        }
    }
    
    public String getString(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
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
    
    @Override
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
    
    // ==================== List 操作 ====================
    
    @Override
    public void rpush(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.rpush(key, value);
        }
    }
    
    @Override
    public void lpush(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.lpush(key, value);
        }
    }
    
    @Override
    public String lpop(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.lpop(key);
        }
    }
    
    @Override
    public long llen(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.llen(key);
        }
    }
    
    @Override
    public void lrem(String key, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.lrem(key, 0, value);
        }
    }
    
    // ==================== Set 操作 ====================
    
    /**
     * 添加元素到 Set
     */
    public long sadd(String key, String... members) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.sadd(key, members);
        }
    }
    
    /**
     * 从 Set 移除元素
     */
    public long srem(String key, String... members) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.srem(key, members);
        }
    }
    
    /**
     * 获取 Set 所有成员
     */
    public java.util.Set<String> smembers(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.smembers(key);
        }
    }
    
    /**
     * 检查元素是否在 Set 中
     */
    public boolean sismember(String key, String member) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.sismember(key, member);
        }
    }
    
    /**
     * 获取 Set 大小
     */
    public long scard(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.scard(key);
        }
    }
    
    // ==================== Sorted Set 操作 ====================
    
    /**
     * 添加元素到 Sorted Set
     */
    public long zadd(String key, double score, String member) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.zadd(key, score, member);
        }
    }
    
    /**
     * 添加元素到 Sorted Set (使用 long 作为 score)
     */
    public long zadd(String key, long userId, long score) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.zadd(key, score, String.valueOf(userId));
        }
    }
    
    /**
     * 从 Sorted Set 移除元素
     */
    public long zrem(String key, String... members) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.zrem(key, members);
        }
    }
    
    /**
     * 获取 Sorted Set 范围内的元素 (按索引)
     */
    public java.util.Set<String> zrange(String key, long start, long stop) {
        try (Jedis jedis = pool.getResource()) {
            java.util.List<String> list = jedis.zrange(key, start, stop);
            return new java.util.LinkedHashSet<>(list);
        }
    }
    
    /**
     * 获取 Sorted Set 范围内的元素 (按分数)
     */
    public java.util.Set<String> zrangeByScore(String key, double min, double max) {
        try (Jedis jedis = pool.getResource()) {
            java.util.List<String> list = jedis.zrangeByScore(key, min, max);
            return new java.util.LinkedHashSet<>(list);
        }
    }
    
    /**
     * 获取元素的分数
     */
    public Double zscore(String key, String member) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.zscore(key, member);
        }
    }
    
    /**
     * 获取 Sorted Set 大小
     */
    public long zcard(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.zcard(key);
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
    
    // ==================== Key 扫描 ====================
    
    /**
     * 扫描匹配的 key（使用 SCAN 命令，生产环境安全）
     * 
     * @param pattern 匹配模式，如 "hall:room:*"
     * @return 匹配的 key 列表
     */
    public java.util.List<String> keys(String pattern) {
        java.util.List<String> result = new java.util.ArrayList<>();
        try (Jedis jedis = pool.getResource()) {
            String cursor = "0";
            redis.clients.jedis.params.ScanParams params = new redis.clients.jedis.params.ScanParams()
                .match(pattern)
                .count(100);
            
            do {
                redis.clients.jedis.resps.ScanResult<String> scanResult = jedis.scan(cursor, params);
                result.addAll(scanResult.getResult());
                cursor = scanResult.getCursor();
            } while (!"0".equals(cursor));
        }
        return result;
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
