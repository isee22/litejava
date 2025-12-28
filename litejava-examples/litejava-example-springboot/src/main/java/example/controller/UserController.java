package example.controller;

import example.model.User;
import example.service.UserService;
import litejava.Context;
import litejava.exception.LiteJavaException;
import litejava.plugins.schedule.Scheduled;
import litejava.util.Maps;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器 - Spring MVC 注解
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Inject
    public UserService userService;
    
    @GetMapping
    public Map<String, Object> list(Context ctx) {
        int page = ctx.queryParamInt("page", 1);
        int size = ctx.queryParamInt("size", 10);
        List<User> users = userService.findAll();
        return Maps.of("list", users, "page", page, "size", size, "total", users.size());
    }
    
    @GetMapping("/{id}")
    public Object get(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new LiteJavaException("User not found: " + id, 404);
        }
        return user;
    }
    
    @PostMapping
    public Map<String, Object> create(@RequestBody User user, Context ctx) {
        if (user.name == null || user.name.isEmpty()) {
            throw new LiteJavaException("Name is required", 400);
        }
        User created = userService.create(user);
        ctx.status(201);
        return Maps.of("data", created, "msg", "Created");
    }
    
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        User existing = userService.findById(id);
        if (existing == null) {
            throw new LiteJavaException("User not found: " + id, 404);
        }
        user.id = id;
        return userService.update(user);
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean ok = userService.delete(id);
        if (!ok) {
            throw new LiteJavaException("User not found: " + id, 404);
        }
        return Maps.of("success", true);
    }
    
    /**
     * 定时任务（注解方式）- SpringBootConfApp 使用
     * 需配置 scheduler.packages: example.controller
     */
    @Scheduled("0 * * * * ?")
    public void statsTask() {
        System.out.println("[Scheduler-Annotation] User count: " + userService.findAll().size());
    }
}
