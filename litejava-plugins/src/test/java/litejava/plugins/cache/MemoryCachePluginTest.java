package litejava.plugins.cache;

import litejava.App;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryCachePlugin 测试
 */
class MemoryCachePluginTest {
    
    static App app;
    static MemoryCachePlugin cache;
    
    @BeforeAll
    static void setup() {
        app = new App();
        cache = new MemoryCachePlugin();
        app.use(cache);
    }
    
    @AfterAll
    static void teardown() {
        if (app != null) app.stop();
    }
    
    @BeforeEach
    void clean() {
        cache.clear();
    }
    
    // ========== 基本操作 ==========
    
    @Test
    void testSetAndGet() {
        cache.set("key1", "value1", 0);
        assertEquals("value1", cache.get("key1"));
    }
    
    @Test
    void testGetNotExists() {
        assertNull(cache.get("not_exists"));
    }
    
    @Test
    void testOverwrite() {
        cache.set("key", "v1", 0);
        cache.set("key", "v2", 0);
        assertEquals("v2", cache.get("key"));
    }
    
    @Test
    void testDelete() {
        cache.set("key", "value", 0);
        cache.del("key");
        assertNull(cache.get("key"));
    }
    
    @Test
    void testExists() {
        assertFalse(cache.exists("key"));
        cache.set("key", "value", 0);
        assertTrue(cache.exists("key"));
    }
    
    // ========== TTL 过期 ==========
    
    @Test
    void testTtlExpire() throws InterruptedException {
        cache.set("ttl_key", "value", 1); // 1秒过期
        assertEquals("value", cache.get("ttl_key"));
        
        Thread.sleep(1100); // 等待过期
        assertNull(cache.get("ttl_key"));
    }
    
    @Test
    void testTtlZeroNeverExpire() throws InterruptedException {
        cache.set("no_ttl", "value", 0); // 永不过期
        Thread.sleep(100);
        assertEquals("value", cache.get("no_ttl"));
    }
    
    // ========== 前缀删除 ==========
    
    @Test
    void testDeleteByPrefix() {
        cache.set("user:1", "a", 0);
        cache.set("user:2", "b", 0);
        cache.set("order:1", "c", 0);
        
        cache.del("user:*");
        
        assertNull(cache.get("user:1"));
        assertNull(cache.get("user:2"));
        assertEquals("c", cache.get("order:1"));
    }
    
    // ========== 通过 instance 访问 ==========
    
    @Test
    void testInstanceAccess() {
        CachePlugin.instance.set("static_key", "static_value", 0);
        assertEquals("static_value", CachePlugin.instance.get("static_key"));
        
        CachePlugin.instance.set("ttl_key", "value", 3600);
        assertTrue(CachePlugin.instance.exists("ttl_key"));
        
        CachePlugin.instance.del("static_key");
        assertFalse(CachePlugin.instance.exists("static_key"));
    }
    
    // ========== Key 前缀 ==========
    
    @Test
    void testKeyPrefix() {
        cache.keyPrefix = "app:";
        cache.set("key", "value", 0);
        
        // 内部存储带前缀
        assertEquals("value", cache.get("key"));
        
        cache.keyPrefix = ""; // 重置
    }
    
    // ========== 边界情况 ==========
    
    @Test
    void testEmptyValue() {
        cache.set("empty", "", 0);
        assertEquals("", cache.get("empty"));
    }
    
    @Test
    void testNullValue() {
        cache.set("null_val", null, 0);
        assertNull(cache.get("null_val"));
    }
    
    @Test
    void testSize() {
        assertEquals(0, cache.size());
        cache.set("k1", "v1", 0);
        cache.set("k2", "v2", 0);
        assertEquals(2, cache.size());
    }
    
    @Test
    void testClear() {
        cache.set("k1", "v1", 0);
        cache.set("k2", "v2", 0);
        cache.clear();
        assertEquals(0, cache.size());
    }
    
    // ========== 对象存储 ==========
    
    @Test
    void testObjectStorage() {
        java.util.Map<String, Object> book = new java.util.HashMap<>();
        book.put("id", 1L);
        book.put("title", "Java编程");
        
        cache.set("book:1", book, 0);
        
        java.util.Map<String, Object> cached = cache.get("book:1");
        assertEquals(1L, cached.get("id"));
        assertEquals("Java编程", cached.get("title"));
    }
    
    @Test
    void testGetOrLoad() {
        // 第一次加载
        String value = cache.getOrLoad("lazy:1", () -> "loaded");
        assertEquals("loaded", value);
        
        // 第二次从缓存获取
        String cached = cache.getOrLoad("lazy:1", () -> "should not load");
        assertEquals("loaded", cached);
    }
}
