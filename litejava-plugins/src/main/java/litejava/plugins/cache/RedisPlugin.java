package litejava.plugins.cache;

import litejava.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis 连接插件 - 管理 Redis 连接池
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * redis:
 *   host: localhost
 *   port: 6379
 *   password:
 *   database: 0
 *   pool:
 *     maxTotal: 50
 *     maxIdle: 10
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式一：使用配置文件
 * RedisPlugin redis = new RedisPlugin();
 * app.use(redis);
 * 
 * // 方式二：代码指定（优先级高于配置文件）
 * RedisPlugin redis = new RedisPlugin();
 * redis.host = "192.168.1.100";
 * redis.password = "secret";
 * app.use(redis);
 * 
 * // 方式三：构造函数
 * RedisPlugin redis = new RedisPlugin("192.168.1.100", 6379, "secret");
 * app.use(redis);
 * }</pre>
 */
public class RedisPlugin extends Plugin {
    
    /** Jedis 连接池 */
    public JedisPool pool;
    
    /** Redis 主机地址 */
    public String host = "localhost";
    
    /** Redis 端口 */
    public int port = 6379;
    
    /** Redis 密码（可选） */
    public String password;
    
    /** Redis 数据库索引 */
    public int database = 0;
    
    /** 连接池最大连接数 */
    public int maxTotal = 50;
    
    /** 连接池最大空闲连接数 */
    public int maxIdle = 10;
    
    /** 连接超时（毫秒） */
    public int timeout = 2000;
    
    public RedisPlugin() {
    }
    
    /**
     * 构造函数 - 指定主机
     */
    public RedisPlugin(String host) {
        this.host = host;
    }
    
    /**
     * 构造函数 - 指定主机和端口
     */
    public RedisPlugin(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 构造函数 - 指定主机、端口和密码
     */
    public RedisPlugin(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }
    
    @Override
    public void config() {
        // 记录原始值，用于检测是否使用默认值
        String originalHost = host;
        int originalPort = port;
        
        // 配置文件覆盖（如果有配置）
        host = app.conf.getString("redis", "host", host);
        port = app.conf.getInt("redis", "port", port);
        password = app.conf.getString("redis", "password", password);
        database = app.conf.getInt("redis", "database", database);
        maxTotal = app.conf.getInt("redis.pool", "maxTotal", maxTotal);
        maxIdle = app.conf.getInt("redis.pool", "maxIdle", maxIdle);
        timeout = app.conf.getInt("redis", "timeout", timeout);
        
        // 如果使用默认值，警告用户
        if (host.equals(originalHost) && port == originalPort && 
            app.conf.getString("redis", "host", null) == null) {
            app.log.warn("RedisPlugin: Using default connection " + host + ":" + port + 
                " (no redis.host configured)");
        }
        
        // 初始化连接池
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        
        if (password != null && !password.isEmpty()) {
            pool = new JedisPool(config, host, port, timeout, password, database);
        } else {
            pool = new JedisPool(config, host, port, timeout, null, database);
        }
        
        app.log.info("RedisPlugin: Connected to " + host + ":" + port + "/" + database);
    }
    
    /**
     * 获取 Jedis 连接（使用后需关闭）
     */
    public Jedis getResource() {
        return pool.getResource();
    }
    
    /**
     * 执行 Redis 命令（自动管理连接）
     */
    public <T> T execute(java.util.function.Function<Jedis, T> action) {
        try (Jedis jedis = pool.getResource()) {
            return action.apply(jedis);
        }
    }
    
    /**
     * 执行 Redis 命令（无返回值）
     */
    public void run(java.util.function.Consumer<Jedis> action) {
        try (Jedis jedis = pool.getResource()) {
            action.accept(jedis);
        }
    }
    
    // ==================== 常用操作快捷方法 ====================
    
    public void set(String key, String value) {
        run(jedis -> jedis.set(key, value));
    }
    
    public void set(String key, String value, int ttlSeconds) {
        run(jedis -> jedis.setex(key, ttlSeconds, value));
    }
    
    public String get(String key) {
        return execute(jedis -> jedis.get(key));
    }
    
    public void del(String... keys) {
        run(jedis -> jedis.del(keys));
    }
    
    public boolean exists(String key) {
        return execute(jedis -> jedis.exists(key));
    }
    
    public void expire(String key, int seconds) {
        run(jedis -> jedis.expire(key, seconds));
    }
    
    public long incr(String key) {
        return execute(jedis -> jedis.incr(key));
    }
    
    public long decr(String key) {
        return execute(jedis -> jedis.decr(key));
    }
    
    @Override
    public void uninstall() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}
