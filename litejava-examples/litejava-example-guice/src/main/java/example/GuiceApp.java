package example;

import com.google.inject.Singleton;
import com.google.inject.name.Names;
import example.service.*;
import example.controller.*;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.di.GuicePlugin;

/**
 * Guice DI 示例 - 展示多种依赖注入方式
 * 
 * <h2>演示内容</h2>
 * <ul>
 *   <li>情况1：接口 + 实现类绑定</li>
 *   <li>情况2：无接口，直接绑定具体类</li>
 *   <li>情况3：@Named 命名绑定（同接口多实现）</li>
 *   <li>情况4：@Singleton 类级别注解（自动单例）</li>
 *   <li>情况5：字段注入 vs 构造函数注入</li>
 * </ul>
 */
public class GuiceApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        // 配置 Guice DI
        GuicePlugin di = new GuicePlugin();
        di.bind(binder -> {
            // ========== 情况1：接口绑定到实现 ==========
            // UserService 是接口，UserServiceImpl 是实现
            binder.bind(UserService.class).to(UserServiceImpl.class).in(Singleton.class);
            
            // ========== 情况2：无接口，直接绑定具体类 ==========
            // ProductService 没有接口，直接绑定自己（单例）
            binder.bind(ProductService.class).in(Singleton.class);
            
            // ========== 情况3：@Named 命名绑定 ==========
            // 同一接口多个实现，用 @Named 区分
            binder.bind(NotificationService.class)
                .annotatedWith(Names.named("email"))
                .to(EmailNotificationService.class);
            binder.bind(NotificationService.class)
                .annotatedWith(Names.named("sms"))
                .to(SmsNotificationService.class);
            
            // ========== 情况4：类上有 @Singleton 注解 ==========
            // OrderService 类上标注了 @Singleton，无需在这里指定作用域
            binder.bind(OrderService.class);
            
            // ========== Controller 不需要绑定 ==========
            // Guice 会自动创建并注入依赖（JIT binding）
        });
        app.use(di);
        
        // 获取 Controller（Guice 自动注入依赖）
        UserController userController = di.get(UserController.class);
        ProductController productController = di.get(ProductController.class);
        OrderController orderController = di.get(OrderController.class);
        
        // 用户路由（演示接口+实现）
        app.group("/users", users -> {
            users.get("/", userController::list);
            users.get("/:id", userController::get);
            users.post("/", userController::create);
        });
        
        // 产品路由（演示无接口直接类）
        app.group("/products", products -> {
            products.get("/", productController::list);
            products.get("/:id", productController::get);
            products.post("/", productController::create);
        });
        
        // 订单路由（演示字段注入 + @Named）
        app.group("/orders", orders -> {
            orders.get("/", orderController::list);
            orders.get("/:id", orderController::get);
            orders.post("/", orderController::create);
        });
        
        // 演示 @Named 获取不同实现
        app.get("/notify/email", ctx -> {
            NotificationService email = di.getNamed("email", NotificationService.class);
            email.send("test@example.com", "Hello from Email!");
            ctx.json(java.util.Map.of("sent", "email"));
        });
        
        app.get("/notify/sms", ctx -> {
            NotificationService sms = di.getNamed("sms", NotificationService.class);
            sms.send("13800138000", "Hello from SMS!");
            ctx.json(java.util.Map.of("sent", "sms"));
        });
        
        app.run();
    }
}
