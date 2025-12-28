package example.controller;

import example.service.UserService;
import litejava.Context;

import javax.inject.Inject;
import java.util.Map;

/**
 * 用户控制器 - 演示构造函数注入
 * 
 * <p>Controller 不需要在类上加任何注解，Guice 会自动：
 * <ol>
 *   <li>发现 @Inject 构造函数</li>
 *   <li>解析构造函数参数的依赖</li>
 *   <li>创建实例并注入依赖</li>
 * </ol>
 * 
 * <p>这种方式叫 JIT (Just-In-Time) Binding，无需显式绑定。
 */
public class UserController {
    
    private final UserService userService;
    
    /**
     * 构造函数注入（推荐）
     * - 依赖明确，一目了然
     * - 便于单元测试（可传入 mock）
     * - 保证依赖不可变（final）
     */
    @Inject
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    public void list(Context ctx) {
        ctx.json(Map.of("list", userService.list()));
    }
    
    public void get(Context ctx) {
        long id = ctx.pathParamLong("id");
        Map<String, Object> user = userService.get(id);
        if (user == null) {
            ctx.status(404).json(Map.of("error", "User not found"));
            return;
        }
        ctx.json(user);
    }
    
    public void create(Context ctx) {
        Map<String, Object> data = ctx.bindJSON();
        Map<String, Object> user = userService.create(data);
        ctx.status(201).json(user);
    }
}
