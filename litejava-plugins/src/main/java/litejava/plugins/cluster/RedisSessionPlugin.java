package litejava.plugins.cluster;

import litejava.Plugin;
import litejava.plugins.cache.RedisPlugin;

import java.util.Map;
import java.util.UUID;

/**
 * Redis Session 插件 - 支持多实例部署的分布式 Session
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * redis:
 *   host: localhost
 *   port: 6379
 * session:
 *   prefix: "session:"
 *   ttl: 1800
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * RedisPlugin redis = new RedisPlugin();
 * app.use(redis);
 * app.use(new RedisSessionPlugin(redis));
 * 
 * // 创建 Session
 * RedisSessionPlugin session = app.getPlugin(RedisSessionPlugin.class);
 * String sessionId = session.createSession(Map.of("userId", "123"));
 * 
 * // 获取 Session
 * Map<String, String> data = session.getSession(sessionId);
 * 
 * // 销毁 Session
 * session.destroySession(sessionId);
 * }</pre>
 */
public class RedisSessionPlugin extends Plugin {
    
    /** RedisPlugin */
    private final RedisPlugin redis;
    
    /** Session key 前缀 */
    public String prefix = "session:";
    
    /** Session 过期时间（秒），默认 30 分钟 */
    public int ttl = 1800;
    
    /**
     * 构造 RedisSessionPlugin
     * 
     * @param redis RedisPlugin 实例（必须）
     */
    public RedisSessionPlugin(RedisPlugin redis) {
        this.redis = redis;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        prefix = app.conf.getString("session", "prefix", prefix);
        ttl = app.conf.getInt("session", "ttl", ttl);
        
        app.log.info("RedisSessionPlugin: prefix=" + prefix + ", ttl=" + ttl + "s");
    }
    
    /**
     * 创建 Session
     * @return sessionId
     */
    public String createSession(Map<String, String> data) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String key = prefix + sessionId;
        redis.run(jedis -> {
            jedis.hset(key, data);
            jedis.expire(key, ttl);
        });
        return sessionId;
    }
    
    /**
     * 获取 Session 数据（自动续期）
     */
    public Map<String, String> getSession(String sessionId) {
        String key = prefix + sessionId;
        return redis.execute(jedis -> {
            Map<String, String> data = jedis.hgetAll(key);
            if (!data.isEmpty()) {
                jedis.expire(key, ttl);
            }
            return data;
        });
    }
    
    /**
     * 设置 Session 字段
     */
    public void setSession(String sessionId, String field, String value) {
        String key = prefix + sessionId;
        redis.run(jedis -> {
            jedis.hset(key, field, value);
            jedis.expire(key, ttl);
        });
    }
    
    /**
     * 设置多个 Session 字段
     */
    public void setSession(String sessionId, Map<String, String> data) {
        String key = prefix + sessionId;
        redis.run(jedis -> {
            jedis.hset(key, data);
            jedis.expire(key, ttl);
        });
    }
    
    /**
     * 获取 Session 单个字段
     */
    public String getSessionField(String sessionId, String field) {
        return redis.execute(jedis -> jedis.hget(prefix + sessionId, field));
    }
    
    /**
     * 删除 Session 字段
     */
    public void removeSessionField(String sessionId, String... fields) {
        redis.run(jedis -> jedis.hdel(prefix + sessionId, fields));
    }
    
    /**
     * 销毁 Session
     */
    public void destroySession(String sessionId) {
        redis.run(jedis -> jedis.del(prefix + sessionId));
    }
    
    /**
     * 检查 Session 是否存在
     */
    public boolean exists(String sessionId) {
        return redis.execute(jedis -> jedis.exists(prefix + sessionId));
    }
    
    /**
     * 刷新 Session 过期时间
     */
    public void refresh(String sessionId) {
        redis.run(jedis -> jedis.expire(prefix + sessionId, ttl));
    }
    
    @Override
    public void uninstall() {
        // 不关闭 RedisPlugin，由 App 统一管理
    }
}
