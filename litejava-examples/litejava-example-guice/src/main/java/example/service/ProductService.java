package example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 产品服务 - 演示无接口的具体类（直接绑定）
 * 
 * <p>不是所有服务都需要接口，简单场景直接用具体类更简洁。
 * Guice 可以直接绑定具体类：{@code binder.bind(ProductService.class).in(Singleton.class)}
 */
public class ProductService {
    
    private final Map<Long, Map<String, Object>> products = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(0);
    
    public ProductService() {
        // 初始化测试数据
        create(Map.of("name", "iPhone 15", "price", 7999));
        create(Map.of("name", "MacBook Pro", "price", 14999));
    }
    
    public List<Map<String, Object>> list() {
        return new ArrayList<>(products.values());
    }
    
    public Map<String, Object> get(long id) {
        return products.get(id);
    }
    
    public Map<String, Object> create(Map<String, Object> data) {
        long id = idGen.incrementAndGet();
        Map<String, Object> product = new HashMap<>(data);
        product.put("id", id);
        product.put("createdat", System.currentTimeMillis());
        products.put(id, product);
        return product;
    }
}
