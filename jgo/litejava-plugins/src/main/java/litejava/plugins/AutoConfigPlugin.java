package litejava.plugins;

import litejava.Plugin;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 自动配置插件 - 通过配置文件启用/禁用插件
 * 
 * <h2>配置示例 (application.yml)</h2>
 * <pre>{@code
 * plugins:
 *   # 用下划线代替点号
 *   litejava_plugins_DebugPlugin: true
 *   litejava_plugins_health_HealthPlugin: true
 *   litejava_plugins_security_CorsPlugin: true
 *   litejava_plugins_http_RecoveryPlugin: true
 *   
 *   # 禁用插件
 *   litejava_plugins_log_RequestLogPlugin: false
 *   
 *   # 带参数的插件
 *   litejava_plugin_StaticFilePlugin:
 *     enabled: true
 *     urlPrefix: /static
 *     directory: static
 *     cacheMaxAge: 86400
 *   
 *   # 用户自定义插件
 *   com_example_MyPlugin: true
 * }</pre>
 */
public class AutoConfigPlugin extends Plugin {
    
    @Override
    @SuppressWarnings("unchecked")
    public void config() {
        Map<String, Object> plugins = app.conf.get("plugins");
        if (plugins == null || plugins.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : plugins.entrySet()) {
            // 下划线转点号
            String className = entry.getKey().replace('_', '.');
            Object value = entry.getValue();
            
            Class<? extends Plugin> pluginClass;
            try {
                Class<?> clazz = Class.forName(className);
                if (!Plugin.class.isAssignableFrom(clazz)) {
                    continue;
                }
                pluginClass = (Class<? extends Plugin>) clazz;
            } catch (ClassNotFoundException e) {
                continue;
            }
            
            String simpleName = pluginClass.getSimpleName();
            
            try {
                if (value instanceof Boolean) {
                    if ((Boolean) value) {
                        if (!app.plugins.containsKey(simpleName)) {
                            app.use(pluginClass.getDeclaredConstructor().newInstance());
                        }
                    } else {
                        app.unuse(pluginClass);
                    }
                } else if (value instanceof Map) {
                    Map<String, Object> cfg = (Map<String, Object>) value;
                    boolean enabled = toBoolean(cfg.getOrDefault("enabled", true));
                    if (enabled && !app.plugins.containsKey(simpleName)) {
                        Plugin p = createAndConfigure(pluginClass, cfg);
                        if (p != null) app.use(p);
                    } else if (!enabled) {
                        app.unuse(pluginClass);
                    }
                }
            } catch (Exception e) {
                // skip
            }
        }
    }
    
    private Plugin createAndConfigure(Class<? extends Plugin> clazz, Map<String, Object> cfg) {
        try {
            Plugin p = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> e : cfg.entrySet()) {
                if ("enabled".equals(e.getKey())) continue;
                try {
                    Field f = findField(clazz, e.getKey());
                    if (f != null) {
                        f.setAccessible(true);
                        f.set(p, convert(e.getValue(), f.getType()));
                    }
                } catch (Exception ex) { }
            }
            return p;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Field findField(Class<?> c, String name) {
        while (c != null && c != Object.class) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        return null;
    }
    
    private Object convert(Object v, Class<?> t) {
        if (v == null || t.isInstance(v)) return v;
        String s = String.valueOf(v);
        if (t == String.class) return s;
        if (t == int.class || t == Integer.class) return Integer.parseInt(s);
        if (t == long.class || t == Long.class) return Long.parseLong(s);
        if (t == boolean.class || t == Boolean.class) return Boolean.parseBoolean(s);
        if (t == double.class || t == Double.class) return Double.parseDouble(s);
        if (t == float.class || t == Float.class) return Float.parseFloat(s);
        return v;
    }
    
    private boolean toBoolean(Object v) {
        return v instanceof Boolean ? (Boolean) v : v != null && Boolean.parseBoolean(String.valueOf(v));
    }
}
