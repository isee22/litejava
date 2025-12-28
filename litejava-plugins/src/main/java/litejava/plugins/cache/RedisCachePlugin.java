package litejava.plugins.cache;

import litejava.plugin.CachePlugin;

/**
 * Redis 缓存插件 - 基于 Jedis 实现，对象自动 JSON 序列化
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * redis:
 *   host: localhost
 *   port: 6379
 *   password:
 *   database: 0
 * cache:
 *   prefix: "cache:"
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * RedisPlugin redis = new RedisPlugin();
 * app.use(redis);
 * app.use(new RedisCachePlugin(redis));
 * 
 * // 通过 app.cache 访问
 * app.cache.set("book:1", book);
 * Map<String, Object> book = app.cache.get("book:1");
 * 
 * // Redis 特有功能
 * RedisCachePlugin redisCache = (RedisCachePlugin) app.cache;
 * redisCache.hset("hash", "field", "value");
 * redisCache.incr("counter");
 * }</pre>
 */
public class RedisCachePlugin extends CachePlugin {
    
    /** RedisPlugin */
    private final RedisPlugin redis;
    
    /**
     * 构造 RedisCachePlugin
     * 
     * @param redis RedisPlugin 实例（必须）
     */
    public RedisCachePlugin(RedisPlugin redis) {
        this.redis = redis;
    }
    
    @Override
    public void config() {
        super.config();
    }
    
    @Override
    public void uninstall() {
        // 不关闭 RedisPlugin，由 App 统一管理
    }
    
    @Override
    public void set(String key, Object value, int ttlSeconds) {
        String json = toJson(value);
        redis.run(jedis -> {
            if (ttlSeconds > 0) {
                jedis.setex(key(key), ttlSeconds, json);
            } else {
                jedis.set(key(key), json);
            }
        });
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        String json = redis.execute(jedis -> jedis.get(key(key)));
        return json != null ? (T) fromJson(json) : null;
    }
    
    @Override
    public void del(String key) {
        redis.run(jedis -> jedis.del(key(key)));
    }
    
    @Override
    public boolean exists(String key) {
        return redis.execute(jedis -> jedis.exists(key(key)));
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
    
    // ==================== Redis 独有操作 ====================
    
    public void expire(String key, int seconds) {
        redis.run(jedis -> jedis.expire(key(key), seconds));
    }
    
    public long ttl(String key) {
        return redis.execute(jedis -> jedis.ttl(key(key)));
    }
    
    public long incr(String key) {
        return redis.execute(jedis -> jedis.incr(key(key)));
    }
    
    public long incrBy(String key, long value) {
        return redis.execute(jedis -> jedis.incrBy(key(key), value));
    }
    
    public long decr(String key) {
        return redis.execute(jedis -> jedis.decr(key(key)));
    }
    
    // ==================== Hash 操作 ====================
    
    public void hset(String key, String field, String value) {
        redis.run(jedis -> jedis.hset(key(key), field, value));
    }
    
    public String hget(String key, String field) {
        return redis.execute(jedis -> jedis.hget(key(key), field));
    }
    
    public void hdel(String key, String... fields) {
        redis.run(jedis -> jedis.hdel(key(key), fields));
    }
    
    public java.util.Map<String, String> hgetAll(String key) {
        return redis.execute(jedis -> jedis.hgetAll(key(key)));
    }
    
    public boolean hexists(String key, String field) {
        return redis.execute(jedis -> jedis.hexists(key(key), field));
    }
    
    // ==================== List 操作 ====================
    
    public void lpush(String key, String... values) {
        redis.run(jedis -> jedis.lpush(key(key), values));
    }
    
    public void rpush(String key, String... values) {
        redis.run(jedis -> jedis.rpush(key(key), values));
    }
    
    public String lpop(String key) {
        return redis.execute(jedis -> jedis.lpop(key(key)));
    }
    
    public String rpop(String key) {
        return redis.execute(jedis -> jedis.rpop(key(key)));
    }
    
    public java.util.List<String> lrange(String key, long start, long stop) {
        return redis.execute(jedis -> jedis.lrange(key(key), start, stop));
    }
    
    public long llen(String key) {
        return redis.execute(jedis -> jedis.llen(key(key)));
    }
    
    // ==================== Set 操作 ====================
    
    public void sadd(String key, String... members) {
        redis.run(jedis -> jedis.sadd(key(key), members));
    }
    
    public void srem(String key, String... members) {
        redis.run(jedis -> jedis.srem(key(key), members));
    }
    
    public boolean sismember(String key, String member) {
        return redis.execute(jedis -> jedis.sismember(key(key), member));
    }
    
    public java.util.Set<String> smembers(String key) {
        return redis.execute(jedis -> jedis.smembers(key(key)));
    }
    
    public long scard(String key) {
        return redis.execute(jedis -> jedis.scard(key(key)));
    }
}
