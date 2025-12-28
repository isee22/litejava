package litejava.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map 工具类 - Java 8 兼容的 Map.of() 替代
 * 
 * <pre>{@code
 * // 代替 Map.of("key", "value")
 * Maps.of("key", "value")
 * 
 * // 代替 Map.of("k1", "v1", "k2", "v2")
 * Maps.of("k1", "v1", "k2", "v2")
 * }</pre>
 */
public final class Maps {
    
    private Maps() {}
    
    /**
     * 创建空的不可变 Map
     */
    public static <K, V> Map<K, V> of() {
        return Collections.emptyMap();
    }
    
    /**
     * 创建包含 1 个键值对的不可变 Map
     */
    public static <K, V> Map<K, V> of(K k1, V v1) {
        return Collections.singletonMap(k1, v1);
    }
    
    /**
     * 创建包含 2 个键值对的不可变 Map
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new LinkedHashMap<>(4);
        map.put(k1, v1);
        map.put(k2, v2);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * 创建包含 3 个键值对的不可变 Map
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new LinkedHashMap<>(4);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * 创建包含 4 个键值对的不可变 Map
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Map<K, V> map = new LinkedHashMap<>(8);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * 创建包含 5 个键值对的不可变 Map
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map<K, V> map = new LinkedHashMap<>(8);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return Collections.unmodifiableMap(map);
    }
    
    /**
     * 创建可变的 LinkedHashMap（保持插入顺序）
     */
    public static <K, V> Map<K, V> newMap() {
        return new LinkedHashMap<>();
    }
    
    /**
     * 创建可变的 LinkedHashMap 并初始化
     */
    public static <K, V> Map<K, V> newMap(K k1, V v1) {
        Map<K, V> map = new LinkedHashMap<>(4);
        map.put(k1, v1);
        return map;
    }
    
    /**
     * 创建可变的 LinkedHashMap 并初始化
     */
    public static <K, V> Map<K, V> newMap(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new LinkedHashMap<>(4);
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    
    /**
     * 创建可变的 LinkedHashMap 并初始化
     */
    public static <K, V> Map<K, V> newMap(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new LinkedHashMap<>(4);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
}
