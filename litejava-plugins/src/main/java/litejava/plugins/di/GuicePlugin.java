package litejava.plugins.di;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import litejava.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Google Guice 依赖注入插件
 * 
 * <h2>方式一：编程式配置 (推荐，符合框架理念)</h2>
 * <pre>{@code
 * GuicePlugin di = new GuicePlugin();
 * di.bind(binder -> {
 *     binder.bind(UserService.class).to(UserServiceImpl.class).in(Singleton.class);
 *     binder.bind(OrderService.class).toInstance(new OrderServiceImpl());
 * });
 * app.use(di);
 * 
 * // 获取实例
 * UserService userService = di.get(UserService.class);
 * }</pre>
 * 
 * <h2>方式二：注解方式 (适合习惯 Spring 的开发者)</h2>
 * <pre>{@code
 * // 服务类使用 @Singleton 和 @Inject
 * @Singleton
 * public class UserServiceImpl implements UserService {
 *     @Inject
 *     public UserServiceImpl(UserRepository repo) {
 *         this.repo = repo;
 *     }
 * }
 * 
 * // 绑定接口到实现
 * GuicePlugin di = new GuicePlugin();
 * di.bind(binder -> {
 *     binder.bind(UserService.class).to(UserServiceImpl.class);
 *     binder.bind(UserRepository.class).to(UserRepositoryImpl.class);
 * });
 * app.use(di);
 * }</pre>
 * 
 * <h2>方式三：命名绑定</h2>
 * <pre>{@code
 * di.bind(binder -> {
 *     binder.bind(DataSource.class).annotatedWith(Names.named("primary")).toInstance(primaryDs);
 *     binder.bind(DataSource.class).annotatedWith(Names.named("secondary")).toInstance(secondaryDs);
 * });
 * 
 * // 获取
 * DataSource primary = di.getNamed("primary", DataSource.class);
 * 
 * // 或在类中注入
 * public class MyService {
 *     @Inject @Named("primary")
 *     DataSource primaryDs;
 * }
 * }</pre>
 * 
 * <h2>方式四：Provider 延迟创建</h2>
 * <pre>{@code
 * di.bind(binder -> {
 *     binder.bind(ExpensiveService.class).toProvider(() -> new ExpensiveService());
 * });
 * }</pre>
 * 
 * <h2>在路由中使用</h2>
 * <pre>{@code
 * app.get("/users", ctx -> {
 *     UserService service = di.get(UserService.class);
 *     ctx.json(service.list());
 * });
 * 
 * // 或注入到 Controller
 * UserController controller = di.get(UserController.class);
 * app.get("/users", controller::list);
 * app.post("/users", controller::create);
 * }</pre>
 * 
 * <h2>Guice 常用注解</h2>
 * <ul>
 *   <li>{@code @Inject} - 构造函数/字段/方法注入</li>
 *   <li>{@code @Singleton} - 单例作用域</li>
 *   <li>{@code @Named("xxx")} - 命名限定符</li>
 *   <li>{@code @Provides} - 在 Module 中提供实例</li>
 * </ul>
 */
public class GuicePlugin extends Plugin {
    
    /** 默认实例（单例访问） */
    public static GuicePlugin instance;
    
    /** Guice Injector，初始化后可用 */
    public Injector injector;
    
    private final List<com.google.inject.Module> modules = new ArrayList<>();
    private final List<Consumer<Binder>> bindings = new ArrayList<>();
    
    public GuicePlugin() {
        instance = this;
    }
    
    /**
     * 添加绑定 (编程式，无注解)
     */
    public GuicePlugin bind(Consumer<Binder> binding) {
        bindings.add(binding);
        return this;
    }
    
    /**
     * 添加 Guice Module
     */
    public GuicePlugin module(com.google.inject.Module module) {
        modules.add(module);
        return this;
    }
    
    @Override
    public void config() {
        // 将编程式绑定转换为 Module
        if (!bindings.isEmpty()) {
            modules.add(new AbstractModule() {
                @Override
                protected void configure() {
                    for (Consumer<Binder> binding : bindings) {
                        binding.accept(binder());
                    }
                }
            });
        }
        
        injector = Guice.createInjector(modules);
        app.log.info("GuicePlugin configured with " + modules.size() + " module(s)");
    }
    
    /**
     * 获取实例
     */
    public <T> T get(Class<T> type) {
        return injector.getInstance(type);
    }
    
    /**
     * 按 Key 获取实例 (用于命名绑定)
     */
    public <T> T get(Key<T> key) {
        return injector.getInstance(key);
    }
    
    /**
     * 按名称获取实例
     */
    public <T> T getNamed(String name, Class<T> type) {
        return injector.getInstance(Key.get(type, Names.named(name)));
    }
    
    /**
     * 注入已有实例的依赖
     */
    public void inject(Object instance) {
        injector.injectMembers(instance);
    }
    
    /**
     * 创建实例并注入依赖
     */
    public <T> T create(Class<T> type) {
        T instance = injector.getInstance(type);
        return instance;
    }
    
    /**
     * 获取 Injector (高级用法)
     */
    public Injector injector() {
        return injector;
    }
}
