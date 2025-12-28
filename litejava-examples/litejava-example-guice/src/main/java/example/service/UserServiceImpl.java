package example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务实现（内存存储，演示用）
 */
public class UserServiceImpl implements UserService {
    
    private final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(0);
    
    public UserServiceImpl() {
        // 初始化测试数据
        create(Map.of("name", "Alice", "email", "alice@example.com"));
        create(Map.of("name", "Bob", "email", "bob@example.com"));
    }
    
    @Override
    public List<Map<String, Object>> list() {
        return new ArrayList<>(users.values());
    }
    
    @Override
    public Map<String, Object> get(long id) {
        return users.get(id);
    }
    
    @Override
    public Map<String, Object> create(Map<String, Object> data) {
        long id = idGen.incrementAndGet();
        Map<String, Object> user = new HashMap<>(data);
        user.put("id", id);
        user.put("createdat", System.currentTimeMillis());
        users.put(id, user);
        return user;
    }
    
    @Override
    public Map<String, Object> update(long id, Map<String, Object> data) {
        Map<String, Object> user = users.get(id);
        if (user == null) return null;
        user.putAll(data);
        user.put("updatedat", System.currentTimeMillis());
        return user;
    }
    
    @Override
    public boolean delete(long id) {
        return users.remove(id) != null;
    }
}
