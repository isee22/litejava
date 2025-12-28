package example.service;

import example.mapper.UserMapper;
import example.model.User;
import litejava.plugins.cache.MemoryCachePlugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * 用户服务
 */
@Singleton
public class UserService {
    
    @Inject
    public UserMapper userMapper;
    
    @Inject
    public MemoryCachePlugin cache;
    
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    public User findById(Long id) {
        String key = "user:" + id;
        User user = cache.get(key);
        if (user != null) {
            return user;
        }
        
        user = userMapper.findById(id);
        if (user != null) {
            cache.set(key, user, 300);
        }
        return user;
    }
    
    public User create(User user) {
        userMapper.insert(user);
        return user;
    }
    
    public User update(User user) {
        userMapper.update(user);
        cache.del("user:" + user.id);
        return user;
    }
    
    public boolean delete(Long id) {
        int rows = userMapper.delete(id);
        cache.del("user:" + id);
        return rows > 0;
    }
}
