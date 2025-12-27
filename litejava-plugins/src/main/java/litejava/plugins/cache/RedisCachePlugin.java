package litejava.plugins.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis 缓存插件 - 基于 Jedis 实现，对象自动 JSON 序列化
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>redis.clients</groupId>
 *     <artifactId>jedis</artifactId>
 *     <version>5.1.0</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>{@code
 * redis:
 *   host: localhost
 *   port: 6379
 *   password:
 *   database: 0
 *   pool:
 *     maxTotal: 8
 *     maxIdle: 8
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new RedisCachePlugin());
 * 
 * // 通过基类 instance 访问 (推荐，便于切换实现)
 * CachePlugin.instance.set("book:1", book);
 * Map<String, Object> book = CachePlugin.instance.get("book:1");
 * 
 * // 需要 Redis 特有功能时，强制转换
 * RedisCachePlugin redis = (RedisCachePlugin) CachePlugin.instance;
 * redis.hset("hash", "field", "value");
 * redis.incr("counter");
 * }</pre>
 */
public class RedisCachePlugin extends CachePlugin {
    
    /** Jedis 连接池，初始化后可用于高级操作 */
    public JedisPool pool;
    
    /** Redis 主机地址，默认 localhost */
    public String host = "localhost";
    
    /** Redis 端口，默认 6379 */
    public int port = 6379;
    
    /** Redis 密码，默认无 */
    public String password;
    
    /** Redis 数据库索引，默认 0 */
    public int database = 0;
    
    /** 连接池最大连接数，默认 8 */
    public int maxTotal = 8;
    
    /** 连接池最大空闲连接数，默认 8 */
    public int maxIdle = 8;
    
    @Override
    public void config() {
        super.config();
        host = app.conf.getString("redis", "host", host);
        port = app.conf.getInt("redis", "port", port);
        password = app.conf.getString("redis", "password", null);
        database = app.conf.getInt("redis", "database", database);
        
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        
        if (password != null && !password.isEmpty()) {
            pool = new JedisPool(config, host, port, 2000, password, database);
        } else {
            pool = new JedisPool(config, host, port, 2000, null, database);
        }
    }
    
    @Override
    public void uninstall() {
        if (pool != null) pool.close();
    }
    
    @Override
    public void set(String key, Object value, int ttlSeconds) {
        String json = toJson(value);
        try (Jedis jedis = pool.getResource()) {
            if (ttlSeconds > 0) {
                jedis.setex(key(key), ttlSeconds, json);
            } else {
                jedis.set(key(key), json);
            }
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try (Jedis jedis = pool.getResource()) {
            String json = jedis.get(key(key));
            return json != null ? (T) fromJson(json) : null;
        }
    }
    
    @Override
    public void del(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key(key));
        }
    }
    
    @Override
    public boolean exists(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.exists(key(key));
        }
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
        return json; // 没有 JsonPlugin 就返回原始字符串
    }
    
    // ==================== Redis 独有操作 ====================
    
    /** 设置过期时间 (秒) */
    public void expire(String key, int seconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.expire(key(key), seconds);
        }
    }
    
    /** 获取剩余过期时间 (秒)，-1 表示永不过期，-2 表示不存在 */
    public long ttl(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.ttl(key(key));
        }
    }
    
    /** 自增 */
    public long incr(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.incr(key(key));
        }
    }
    
    /** 自增指定值 */
    public long incrBy(String key, long value) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.incrBy(key(key), value);
        }
    }
    
    /** 自减 */
    public long decr(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.decr(key(key));
        }
    }
    
    // ==================== Hash 操作 ====================
    
    /** Hash 设置 */
    public void hset(String key, String field, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key(key), field, value);
        }
    }
    
    /** Hash 获取 */
    public String hget(String key, String field) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget(key(key), field);
        }
    }
    
    /** Hash 删除 */
    public void hdel(String key, String... fields) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hdel(key(key), fields);
        }
    }
    
    /** Hash 获取全部 */
    public java.util.Map<String, String> hgetAll(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hgetAll(key(key));
        }
    }
    
    /** Hash 字段是否存在 */
    public boolean hexists(String key, String field) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hexists(key(key), field);
        }
    }
    
    // ==================== List 操作 ====================
    
    /** List 左侧插入 */
    public void lpush(String key, String... values) {
        try (Jedis jedis = pool.getResource()) {
            jedis.lpush(key(key), values);
        }
    }
    
    /** List 右侧插入 */
    public void rpush(String key, String... values) {
        try (Jedis jedis = pool.getResource()) {
            jedis.rpush(key(key), values);
        }
    }
    
    /** List 左侧弹出 */
    public String lpop(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.lpop(key(key));
        }
    }
    
    /** List 右侧弹出 */
    public String rpop(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.rpop(key(key));
        }
    }
    
    /** List 范围获取 */
    public java.util.List<String> lrange(String key, long start, long stop) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.lrange(key(key), start, stop);
        }
    }
    
    /** List 长度 */
    public long llen(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.llen(key(key));
        }
    }
    
    // ==================== Set 操作 ====================
    
    /** Set 添加 */
    public void sadd(String key, String... members) {
        try (Jedis jedis = pool.getResource()) {
            jedis.sadd(key(key), members);
        }
    }
    
    /** Set 移除 */
    public void srem(String key, String... members) {
        try (Jedis jedis = pool.getResource()) {
            jedis.srem(key(key), members);
        }
    }
    
    /** Set 是否包含 */
    public boolean sismember(String key, String member) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.sismember(key(key), member);
        }
    }
    
    /** Set 获取全部 */
    public java.util.Set<String> smembers(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.smembers(key(key));
        }
    }
    
    /** Set 大小 */
    public long scard(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.scard(key(key));
        }
    }
}
