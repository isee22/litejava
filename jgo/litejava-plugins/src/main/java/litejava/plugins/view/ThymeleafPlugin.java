package litejava.plugins.view;

import litejava.plugin.ViewPlugin;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.util.Map;

/**
 * Thymeleaf 模板插件 - Spring 生态的自然模板引擎
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.thymeleaf</groupId>
 *     <artifactId>thymeleaf</artifactId>
 *     <version>3.1.2.RELEASE</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用 classpath (推荐)
 * app.use(new ThymeleafPlugin("templates/"));
 * 
 * // 使用文件系统绝对路径
 * app.use(new ThymeleafPlugin("/path/to/templates/", true));
 * 
 * // 在 Handler 中
 * ctx.render("index", Map.of("title", "Hello", "items", list));
 * }</pre>
 * 
 * <h2>性能优化</h2>
 * <ul>
 *   <li>默认启用模板缓存（生产环境必须）</li>
 *   <li>显式设置 TemplateMode.HTML（避免自动检测开销）</li>
 *   <li>支持预热模板（启动时编译常用模板）</li>
 * </ul>
 */
public class ThymeleafPlugin extends ViewPlugin {
    
    public static ThymeleafPlugin instance;
    
    public TemplateEngine engine;
    public String templateDir = "templates/";
    public String suffix = ".html";
    public boolean useFileSystem = false;
    
    /** 是否启用缓存，默认 true（生产环境必须启用） */
    public boolean cacheable = true;
    
    /** 缓存 TTL（毫秒），null 表示永不过期 */
    public Long cacheTTL = null;
    
    /** 预热模板列表（启动时预编译） */
    public String[] warmupTemplates = null;
    
    
    public ThymeleafPlugin() {}
    
    public ThymeleafPlugin(String templateDir) {
        this.templateDir = templateDir.endsWith("/") ? templateDir : templateDir + "/";
        // 自动检测：如果是绝对路径或存在的目录，使用文件系统
        this.useFileSystem = new File(templateDir).isAbsolute() || new File(templateDir).exists();
    }
    
    public ThymeleafPlugin(String templateDir, boolean useFileSystem) {
        this.templateDir = templateDir.endsWith("/") ? templateDir : templateDir + "/";
        this.useFileSystem = useFileSystem;
    }
    
    /**
     * 设置预热模板（启动时预编译，提升首次访问性能）
     * @param templates 模板名列表（不含后缀）
     * @return this
     */
    public ThymeleafPlugin warmup(String... templates) {
        this.warmupTemplates = templates;
        return this;
    }
    
    /**
     * 设置缓存配置
     * @param cacheable 是否启用缓存
     * @return this
     */
    public ThymeleafPlugin cache(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }
    
    @Override
    public void config() {
        engine = new TemplateEngine();
        
        if (useFileSystem) {
            FileTemplateResolver resolver = new FileTemplateResolver();
            resolver.setPrefix(templateDir);
            resolver.setSuffix(suffix);
            resolver.setTemplateMode(TemplateMode.HTML);  // 显式设置，避免自动检测
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(cacheable);
            if (cacheTTL != null) {
                resolver.setCacheTTLMs(cacheTTL);
            }
            engine.setTemplateResolver(resolver);
        } else {
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix(templateDir);
            resolver.setSuffix(suffix);
            resolver.setTemplateMode(TemplateMode.HTML);  // 显式设置，避免自动检测
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(cacheable);
            if (cacheTTL != null) {
                resolver.setCacheTTLMs(cacheTTL);
            }
            engine.setTemplateResolver(resolver);
        }
        
        // 预热模板
        if (warmupTemplates != null && warmupTemplates.length > 0) {
            Context ctx = new Context();
            for (String template : warmupTemplates) {
                try {
                    engine.process(template, ctx);
                } catch (Exception e) {
                    // 预热失败不影响启动
                    if (app.devMode) {
                        app.log.warn("Failed to warmup template: " + template);
                    }
                }
            }
        }
        
        if (instance == null) instance = this;
    }
    
    @Override
    public String render(String template, Map<String, Object> data) {
        // 每次创建新 Context（与 Javalin 一致，避免 ThreadLocal 开销）
        Context ctx = new Context();
        ctx.setVariables(data);
        
        // 如果模板名已经带后缀，去掉它（避免 .html.html）
        if (template.endsWith(suffix)) {
            template = template.substring(0, template.length() - suffix.length());
        }
        return engine.process(template, ctx);
    }
}
