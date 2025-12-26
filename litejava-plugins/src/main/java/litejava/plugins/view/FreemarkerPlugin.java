package litejava.plugins.view;

import freemarker.template.Configuration;
import freemarker.template.Template;
import litejava.plugin.ViewPlugin;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;

/**
 * Freemarker 模板插件 - 功能强大的老牌模板引擎
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.freemarker</groupId>
 *     <artifactId>freemarker</artifactId>
 *     <version>2.3.32</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new FreemarkerPlugin("templates"));
 * 
 * // 在 Handler 中
 * ctx.render("index", Map.of("title", "Hello", "items", list));
 * 
 * // 模板文件: templates/index.ftl
 * // <h1>${title}</h1>
 * // <ul><#list items as item><li>${item}</li></#list></ul>
 * }</pre>
 */
public class FreemarkerPlugin extends ViewPlugin {
    
    /** 默认实例（单例访问） */
    public static FreemarkerPlugin instance;
    
    public Configuration cfg;
    public String templateDir = "templates";
    
    public FreemarkerPlugin() {}
    
    public FreemarkerPlugin(String templateDir) {
        this.templateDir = templateDir;
    }
    
    @Override
    public void config() {
        try {
            cfg = new Configuration(Configuration.VERSION_2_3_32);
            cfg.setDirectoryForTemplateLoading(new File(templateDir));
            cfg.setDefaultEncoding("UTF-8");
            
            if (instance == null) instance = this;
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Freemarker", e);
        }
    }
    
    @Override
    public String render(String template, Map<String, Object> data) {
        try {
            Template tpl = cfg.getTemplate(template + ".ftl");
            StringWriter out = new StringWriter();
            tpl.process(data, out);
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render template: " + template, e);
        }
    }
}
