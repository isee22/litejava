package litejava.plugins.i18n;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 国际化插件 - 多语言支持
 * 
 * <h2>依赖</h2>
 * 无外部依赖
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * I18nPlugin i18n = new I18nPlugin();
 * i18n.defaultLocale = "zh-CN";
 * i18n.loadDir("locales");  // 加载 locales/ 目录下的语言文件
 * app.use(i18n);
 * 
 * // 在 Handler 中使用
 * app.get("/hello", ctx -> {
 *     String msg = i18n.t(ctx, "hello");  // 根据请求语言返回
 *     ctx.text(msg);
 * });
 * 
 * // 带参数
 * i18n.t(ctx, "welcome", "张三");  // welcome=欢迎, {0}!
 * 
 * // 直接指定语言
 * i18n.t("en", "hello");
 * }</pre>
 * 
 * <h2>语言文件格式</h2>
 * <pre>
 * locales/
 *   zh-CN.properties
 *   en.properties
 *   ja.properties
 * 
 * # zh-CN.properties
 * hello=你好
 * welcome=欢迎, {0}!
 * items.count=共 {0} 条记录
 * </pre>
 * 
 * <h2>语言检测顺序</h2>
 * <ol>
 *   <li>URL 参数: ?lang=en</li>
 *   <li>Cookie: lang=en</li>
 *   <li>Accept-Language 请求头</li>
 *   <li>默认语言</li>
 * </ol>
 */
public class I18nPlugin extends MiddlewarePlugin {
    
    /** 默认实例（单例访问） */
    public static I18nPlugin instance;
    
    private final Map<String, Map<String, String>> messages = new ConcurrentHashMap<>();
    public String defaultLocale = "en";
    public String queryParam = "lang";
    public String cookieName = "lang";
    
    public I18nPlugin() {
        instance = this;
    }
    
    @Override
    public void uninstall() {
        messages.clear();
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        // 检测语言
        String locale = detectLocale(ctx);
        ctx.state.put("locale", locale);
        ctx.state.put("i18n", this);
        next.run();
    }
    
    /**
     * 加载语言目录
     */
    public I18nPlugin loadDir(String dir) {
        File folder = new File(dir);
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("I18n directory not found: " + dir);
            return this;
        }
        
        File[] files = folder.listFiles((d, name) -> name.endsWith(".properties"));
        if (files != null) {
            for (File file : files) {
                String locale = file.getName().replace(".properties", "");
                loadFile(locale, file);
            }
        }
        return this;
    }
    
    /**
     * 加载单个语言文件
     */
    public I18nPlugin loadFile(String locale, File file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            Properties props = new Properties();
            props.load(reader);
            
            Map<String, String> map = messages.computeIfAbsent(locale, k -> new ConcurrentHashMap<>());
            for (String key : props.stringPropertyNames()) {
                map.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            System.err.println("Failed to load i18n file: " + file + " - " + e.getMessage());
        }
        return this;
    }
    
    /**
     * 从 classpath 加载
     */
    public I18nPlugin loadResource(String locale, String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("I18n resource not found: " + resourcePath);
                return this;
            }
            Properties props = new Properties();
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            
            Map<String, String> map = messages.computeIfAbsent(locale, k -> new ConcurrentHashMap<>());
            for (String key : props.stringPropertyNames()) {
                map.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            System.err.println("Failed to load i18n resource: " + resourcePath);
        }
        return this;
    }
    
    /**
     * 手动添加翻译
     */
    public I18nPlugin add(String locale, String key, String value) {
        messages.computeIfAbsent(locale, k -> new ConcurrentHashMap<>()).put(key, value);
        return this;
    }
    
    /**
     * 翻译 (根据 Context 自动检测语言)
     */
    public String t(Context ctx, String key, Object... args) {
        String locale = (String) ctx.state.getOrDefault("locale", defaultLocale);
        return t(locale, key, args);
    }
    
    /**
     * 翻译 (指定语言)
     */
    public String t(String locale, String key, Object... args) {
        // 尝试完整 locale
        String msg = getMessage(locale, key);
        
        // 尝试语言部分 (zh-CN -> zh)
        if (msg == null && locale.contains("-")) {
            msg = getMessage(locale.split("-")[0], key);
        }
        
        // 尝试默认语言
        if (msg == null) {
            msg = getMessage(defaultLocale, key);
        }
        
        // 返回 key 本身
        if (msg == null) {
            return key;
        }
        
        // 格式化参数
        if (args.length > 0) {
            return MessageFormat.format(msg, args);
        }
        return msg;
    }
    
    private String getMessage(String locale, String key) {
        Map<String, String> map = messages.get(locale);
        return map != null ? map.get(key) : null;
    }
    
    /**
     * 检测请求语言
     */
    public String detectLocale(Context ctx) {
        // 1. URL 参数
        String lang = ctx.queryParam(queryParam);
        if (lang != null && !lang.isEmpty()) {
            return lang;
        }
        
        // 2. Cookie
        String cookie = ctx.headers.get("Cookie");
        if (cookie != null) {
            for (String part : cookie.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && cookieName.equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        
        // 3. Accept-Language
        String acceptLang = ctx.headers.get("Accept-Language");
        if (acceptLang != null && !acceptLang.isEmpty()) {
            // 解析 Accept-Language: zh-CN,zh;q=0.9,en;q=0.8
            String[] parts = acceptLang.split(",");
            if (parts.length > 0) {
                String first = parts[0].split(";")[0].trim();
                return first;
            }
        }
        
        // 4. 默认
        return defaultLocale;
    }
    
    /**
     * 获取所有支持的语言
     */
    public Set<String> getLocales() {
        return messages.keySet();
    }
}
