package example.service;

import example.mapper.UserMapper;
import example.model.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * 用户服务 - 使用 Spring Cache 注解
 * 
 * 演示如何在 LiteJava 中使用 @Cacheable/@CacheEvict/@CachePut 注解
 */
@Singleton
public class CachedUserService {
    
    @Inject
    public UserMapper userMapper;
    
    /**
     * 查询所有用户（缓存 5 分钟）
     */
    @Cacheable(value = "users", key = "'all'")
    public List<User> findAll() {
        System.out.println("[CachedUserService] findAll() - 从数据库查询");
        return userMapper.findAll();
    }
    
    /**
     * 根据 ID 查询用户（自动缓存）
     */
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        System.out.println("[CachedUserService] findById(" + id + ") - 从数据库查询");
        return userMapper.findById(id);
    }
    
    /**
     * 创建用户（清除列表缓存）
     */
    @CacheEvict(value = "users", key = "'all'")
    public User create(User user) {
        System.out.println("[CachedUserService] create() - 清除 users:all 缓存");
        userMapper.insert(user);
        return user;
    }
    
    /**
     * 更新用户（更新缓存）
     */
    @CachePut(value = "users", key = "#user.id")
    @CacheEvict(value = "users", key = "'all'")
    public User update(User user) {
        System.out.println("[CachedUserService] update() - 更新 users:" + user.id + " 缓存");
        userMapper.update(user);
        return user;
    }
    
    /**
     * 删除用户（清除缓存）
     */
    @CacheEvict(value = "users", allEntries = true)
    public boolean delete(Long id) {
        System.out.println("[CachedUserService] delete() - 清除所有 users 缓存");
        int rows = userMapper.delete(id);
        return rows > 0;
    }
}
