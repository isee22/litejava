package litejava.plugins.di;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import litejava.Plugin;
import litejava.plugin.ClassScanner;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Google Guice 依赖注入插件
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * guice:
 *   packages: com.example.service,com.example.repository
 *   autoBindPlugins: true
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Singleton
 * public class UserService {
 *     @Inject MemoryCachePlugin cache;
 * }
 * 
 * app.use(new GuicePlugin());
 * }</pre>
 */
public class GuicePlugin extends Plugin {
    
    /** Guice Injector */
    public Injector injector;
    
    /** 扫描包（逗号分隔） */
    public String packages;
    
    /** 自动绑定 App 中的插件实例 */
    public boolean autoBindPlugins = true;
    
    private final List<com.google.inject.Module> modules = new ArrayList<>();
    private final List<Consumer<Binder>> bindings = new ArrayList<>();
    
    public GuicePlugin() {
    }
    
    /**
     * 添加绑定
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
    @SuppressWarnings("unchecked")
    public void config() {
        packages = app.conf.getString("guice", "packages", packages);
        autoBindPlugins = app.conf.getBool("guice", "autoBindPlugins", autoBindPlugins);
        
        // 自动绑定 App 中的插件实例
        if (autoBindPlugins) {
            modules.add(new AbstractModule() {
                @Override
                protected void configure() {
                    for (Plugin plugin : app.plugins.values()) {
                        if (plugin != GuicePlugin.this) {
                            bind((Class<Plugin>) plugin.getClass()).toInstance(plugin);
                        }
                    }
                }
            });
        }
        
        // 扫描包中的 @Singleton 类
        if (packages != null && !packages.isEmpty()) {
            List<Class<?>> singletonClasses = new ArrayList<>();
            ClassScanner scanner = ClassScanner.getInstance();
            
            for (String pkg : packages.split(",")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    for (Class<?> clazz : scanner.scan(trimmed)) {
                        if (clazz.isAnnotationPresent(Singleton.class) || 
                            clazz.isAnnotationPresent(com.google.inject.Singleton.class)) {
                            singletonClasses.add(clazz);
                        }
                    }
                }
            }
            
            if (!singletonClasses.isEmpty()) {
                modules.add(new AbstractModule() {
                    @Override
                    protected void configure() {
                        for (Class<?> clazz : singletonClasses) {
                            bind(clazz).in(com.google.inject.Singleton.class);
                        }
                    }
                });
            }
        }
        
        // 编程式绑定
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
        app.log.info("GuicePlugin: Ready");
    }
    
    public <T> T get(Class<T> type) {
        return injector.getInstance(type);
    }
    
    public <T> T get(Key<T> key) {
        return injector.getInstance(key);
    }
    
    public <T> T getNamed(String name, Class<T> type) {
        return injector.getInstance(Key.get(type, Names.named(name)));
    }
    
    public void inject(Object instance) {
        injector.injectMembers(instance);
    }
}
