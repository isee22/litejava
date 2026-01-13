package game.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import litejava.plugins.cache.CachePlugin;

/**
 * 缓存工具类 (全局单例，类似 DB)
 */
public class Cache {

    public static CachePlugin instance;
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final int EXPIRE = 30 * 24 * 3600; // 30天

    /**
     * 获取对象 (自动反序列化)
     */
    public static <T> T get(String key, Class<T> clazz) {
        if (instance == null) return null;
        String json = instance.get(key);
        if (json == null) return null;
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 存储对象 (自动序列化，默认30天过期)
     */
    public static void set(String key, Object value) {
        set(key, value, EXPIRE);
    }

    /**
     * 存储对象 (自动序列化，指定过期时间)
     */
    public static void set(String key, Object value, int expireSeconds) {
        if (instance == null || value == null) return;
        try {
            instance.set(key, mapper.writeValueAsString(value), expireSeconds);
        } catch (Exception ignored) {
        }
    }

    /**
     * 删除
     */
    public static void del(String key) {
        if (instance != null) {
            instance.del(key);
        }
    }

    /**
     * 是否存在
     */
    public static boolean exists(String key) {
        return instance != null && instance.exists(key);
    }

    /**
     * 获取原始字符串
     */
    public static String getString(String key) {
        return instance != null ? instance.get(key) : null;
    }

    /**
     * 设置原始字符串 (默认30天过期)
     */
    public static void setString(String key, String value) {
        setString(key, value, EXPIRE);
    }

    /**
     * 设置原始字符串 (指定过期时间)
     */
    public static void setString(String key, String value, int expireSeconds) {
        if (instance != null) {
            instance.set(key, value, expireSeconds);
        }
    }

    /**
     * 原子递增
     */
    public static long incr(String key) {
        return instance != null ? instance.incr(key) : 0;
    }
}

