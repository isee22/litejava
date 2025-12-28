package litejava.plugins.cache;

import litejava.Plugin;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Spring Cache 插件 - 支持 @Cacheable/@CacheEvict/@CachePut 注解
 * 
 * <p>集成 Spring Cache 抽象，让用户可以使用 Spring 缓存注解。
 * 底层缓存可以使用 LiteJava 的 MemoryCachePlugin 或 RedisCachePlugin。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 创建 Spring Cache 插件
 * SpringCachePlugin springCache = new SpringCachePlugin();
 * app.use(springCache);
 * 
 * // 2. 注册需要缓存代理的服务类
 * UserService userService = springCache.proxy(UserServiceImpl.class);
 * 
 * // 3. 在服务类中使用 Spring 缓存注解
 * public class UserServiceImpl implements UserService {
 *     @Cacheable(value = "users", key = "#id")
 *     public User findById(Long id) {
 *         return db.query("SELECT * FROM users WHERE id = ?", id);
 *     }
 *     
 *     @CacheEvict(value = "users", key = "#id")
 *     public void deleteById(Long id) {
 *         db.update("DELETE FROM users WHERE id = ?", id);
 *     }
 *     
 *     @CachePut(value = "users", key = "#user.id")
 *     public User update(User user) {
 *         db.update("UPDATE users SET name = ? WHERE id = ?", user.name, user.id);
 *         return user;
 *     }
 * }
 * 
 * // 4. 使用 Redis 作为缓存后端
 * RedisCachePlugin redis = new RedisCachePlugin();
 * app.use(redis);
 * SpringCachePlugin springCache = new SpringCachePlugin(redis);
 * app.use(springCache);
 * }</pre>
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * spring.cache:
 *   cacheNames: users,products,orders
 *   defaultExpiration: 3600
 * </pre>
 */
public class SpringCachePlugin extends Plugin {
    
    public static SpringCachePlugin instance;
    
    /** Spring 应用上下文 */
    public AnnotationConfigApplicationContext context;
    
    /** Spring CacheManager */
    public CacheManager cacheManager;
    
    /** 底层缓存插件（可选） */
    private CacheBackend backend;
    
    /** 缓存名称列表 */
    public List<String> cacheNames = new ArrayList<>();
    
    /** 默认过期时间（秒） */
    public int defaultExpiration = 3600;
    
    /** 已代理的 Bean */
    private Map<Class<?>, Object> proxiedBeans = new ConcurrentHashMap<>();
    
    /**
     * 缓存后端接口
     */
    public interface CacheBackend {
        Object get(String cacheName, Object key);
        void put(String cacheName, Object key, Object value);
        void evict(String cacheName, Object key);
        void clear(String cacheName);
    }
    
    public SpringCachePlugin() {
    }
    
    /**
     * 使用 MemoryCachePlugin 作为后端
     */
    public SpringCachePlugin(MemoryCachePlugin memoryCache) {
        this.backend = new MemoryCacheBackend(memoryCache);
    }
    
    /**
     * 使用 RedisCachePlugin 作为后端
     */
    public SpringCachePlugin(RedisCachePlugin redisCache) {
        this.backend = new RedisCacheBackend(redisCache);
    }
    
    /**
     * 使用自定义后端
     */
    public SpringCachePlugin(CacheBackend backend) {
        this.backend = backend;
    }
    
    @Override
    public void config() {
        instance = this;
        
        // 从配置文件读取
        String names = app.conf.getString("spring.cache", "cacheNames", "");
        if (!names.isEmpty()) {
            for (String name : names.split(",")) {
                cacheNames.add(name.trim());
            }
        }
        defaultExpiration = app.conf.getInt("spring.cache", "defaultExpiration", defaultExpiration);
        
        // 创建 Spring 上下文
        context = new AnnotationConfigApplicationContext();
        context.register(CacheConfig.class);
        
        // 注册 CacheManager Bean
        context.registerBean("cacheManager", CacheManager.class, () -> createCacheManager());
        
        context.refresh();
        
        cacheManager = context.getBean(CacheManager.class);
        
        app.log.info("SpringCachePlugin: Ready with " + cacheNames.size() + " caches");
    }
    
    /**
     * 创建 CacheManager
     */
    private CacheManager createCacheManager() {
        if (backend != null) {
            return new LiteJavaCacheManager(backend, cacheNames);
        } else {
            // 使用 Spring 默认的 ConcurrentMapCache
            SimpleCacheManager manager = new SimpleCacheManager();
            List<Cache> caches = new ArrayList<>();
            for (String name : cacheNames) {
                caches.add(new ConcurrentMapCache(name));
            }
            if (caches.isEmpty()) {
                caches.add(new ConcurrentMapCache("default"));
            }
            manager.setCaches(caches);
            manager.afterPropertiesSet();
            return manager;
        }
    }
    
    /**
     * 创建带缓存注解支持的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> clazz) {
        return proxy(clazz, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance of " + clazz, e);
            }
        });
    }
    
    /**
     * 创建带缓存注解支持的代理对象（使用工厂）
     */
    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> clazz, Supplier<T> factory) {
        return (T) proxiedBeans.computeIfAbsent(clazz, k -> {
            context.registerBean(clazz, factory::get);
            return context.getBean(clazz);
        });
    }
    
    /**
     * 获取 Cache 实例
     */
    public Cache getCache(String name) {
        return cacheManager.getCache(name);
    }
    
    /**
     * 清空指定缓存
     */
    public void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAll() {
        for (String name : cacheNames) {
            clear(name);
        }
    }
    
    @Override
    public void uninstall() {
        if (context != null) {
            context.close();
        }
        proxiedBeans.clear();
        instance = null;
    }
    
    // ==================== Spring 配置类 ====================
    
    @Configuration
    @EnableCaching
    public static class CacheConfig {
    }
    
    // ==================== LiteJava CacheManager 实现 ====================
    
    /**
     * 基于 LiteJava CacheBackend 的 CacheManager
     */
    private static class LiteJavaCacheManager implements CacheManager {
        
        private final CacheBackend backend;
        private final Map<String, Cache> caches = new ConcurrentHashMap<>();
        private final Collection<String> cacheNames;
        
        public LiteJavaCacheManager(CacheBackend backend, Collection<String> cacheNames) {
            this.backend = backend;
            this.cacheNames = cacheNames;
            for (String name : cacheNames) {
                caches.put(name, new LiteJavaCache(name, backend));
            }
        }
        
        @Override
        public Cache getCache(String name) {
            return caches.computeIfAbsent(name, n -> new LiteJavaCache(n, backend));
        }
        
        @Override
        public Collection<String> getCacheNames() {
            return cacheNames;
        }
    }
    
    /**
     * 基于 LiteJava CacheBackend 的 Cache
     */
    private static class LiteJavaCache implements Cache {
        
        private final String name;
        private final CacheBackend backend;
        
        public LiteJavaCache(String name, CacheBackend backend) {
            this.name = name;
            this.backend = backend;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public Object getNativeCache() {
            return backend;
        }
        
        @Override
        public ValueWrapper get(Object key) {
            Object value = backend.get(name, key);
            return value != null ? () -> value : null;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            Object value = backend.get(name, key);
            return (T) value;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            Object value = backend.get(name, key);
            if (value != null) {
                return (T) value;
            }
            try {
                T newValue = valueLoader.call();
                put(key, newValue);
                return newValue;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public void put(Object key, Object value) {
            backend.put(name, key, value);
        }
        
        @Override
        public void evict(Object key) {
            backend.evict(name, key);
        }
        
        @Override
        public void clear() {
            backend.clear(name);
        }
    }
    
    // ==================== 缓存后端实现 ====================
    
    /**
     * MemoryCachePlugin 后端
     */
    private static class MemoryCacheBackend implements CacheBackend {
        private final MemoryCachePlugin cache;
        
        public MemoryCacheBackend(MemoryCachePlugin cache) {
            this.cache = cache;
        }
        
        @Override
        public Object get(String cacheName, Object key) {
            return cache.get(cacheName + ":" + key);
        }
        
        @Override
        public void put(String cacheName, Object key, Object value) {
            cache.set(cacheName + ":" + key, value);
        }
        
        @Override
        public void evict(String cacheName, Object key) {
            cache.del(cacheName + ":" + key);
        }
        
        @Override
        public void clear(String cacheName) {
            cache.del(cacheName + ":*");
        }
    }
    
    /**
     * RedisCachePlugin 后端
     */
    private static class RedisCacheBackend implements CacheBackend {
        private final RedisCachePlugin cache;
        
        public RedisCacheBackend(RedisCachePlugin cache) {
            this.cache = cache;
        }
        
        @Override
        public Object get(String cacheName, Object key) {
            return cache.get(cacheName + ":" + key);
        }
        
        @Override
        public void put(String cacheName, Object key, Object value) {
            cache.set(cacheName + ":" + key, value);
        }
        
        @Override
        public void evict(String cacheName, Object key) {
            cache.del(cacheName + ":" + key);
        }
        
        @Override
        public void clear(String cacheName) {
            cache.del(cacheName + ":*");
        }
    }
}
