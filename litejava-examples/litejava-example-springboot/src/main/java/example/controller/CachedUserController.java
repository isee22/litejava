package example.controller;

import example.model.User;
import example.service.CachedUserService;
import litejava.exception.LiteJavaException;
import litejava.plugins.cache.SpringCachePlugin;
import litejava.util.Maps;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Map;

/**
 * 缓存用户控制器 - 演示 @Cacheable 注解
 * 
 * 访问 /api/cached/users 会自动缓存结果
 */
@RestController
@RequestMapping("/api/cached/users")
public class CachedUserController {
    
    @Inject
    public SpringCachePlugin springCache;
    
    /** 通过 SpringCachePlugin 代理的 Service，支持 @Cacheable 注解 */
    private CachedUserService cachedService;
    
    private CachedUserService getService() {
        if (cachedService == null) {
            cachedService = springCache.proxy(CachedUserService.class);
        }
        return cachedService;
    }
    
    /**
     * 获取用户列表（带缓存）
     * 第一次调用会查询数据库，后续调用直接返回缓存
     */
    @GetMapping
    public Map<String, Object> list() {
        return Maps.of("list", getService().findAll(), "cached", true);
    }
    
    /**
     * 获取单个用户（带缓存）
     */
    @GetMapping("/{id}")
    public Object get(@PathVariable Long id) {
        User user = getService().findById(id);
        if (user == null) {
            throw new LiteJavaException("User not found: " + id, 404);
        }
        return user;
    }
    
    /**
     * 创建用户（清除列表缓存）
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody User user) {
        User created = getService().create(user);
        return Maps.of("data", created, "msg", "Created, cache cleared");
    }
    
    /**
     * 更新用户（更新缓存）
     */
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        user.id = id;
        return getService().update(user);
    }
    
    /**
     * 删除用户（清除缓存）
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean ok = getService().delete(id);
        if (!ok) {
            throw new LiteJavaException("User not found: " + id, 404);
        }
        return Maps.of("success", true, "msg", "Deleted, cache cleared");
    }
}
