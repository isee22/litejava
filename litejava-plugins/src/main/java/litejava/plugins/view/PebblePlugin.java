package litejava.plugins.view;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import litejava.plugin.ViewPlugin;

import java.io.StringWriter;
import java.util.Map;

/**
 * Pebble 模板插件 - 轻量级模板引擎，语法类似 Twig
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.pebbletemplates</groupId>
 *     <artifactId>pebble</artifactId>
 *     <version>3.2.2</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new PebblePlugin("templates"));
 * 
 * // 在 Handler 中
 * ctx.render("index", Map.of("title", "Hello", "items", list));
 * 
 * // 模板文件: templates/index.peb
 * // <h1>{{ title }}</h1>
 * // <ul>{% for item in items %}<li>{{ item }}</li>{% endfor %}</ul>
 * }</pre>
 */
public class PebblePlugin extends ViewPlugin {
    
    public PebbleEngine engine;
    public String templateDir = "templates";
    public String suffix = ".peb";
    
    public PebblePlugin() {}
    
    public PebblePlugin(String templateDir) {
        this.templateDir = templateDir;
    }
    
    @Override
    public void config() {
        engine = new PebbleEngine.Builder()
            .cacheActive(!app.devMode)
            .build();
    }
    
    @Override
    public String render(String template, Map<String, Object> data) {
        try {
            PebbleTemplate tpl = engine.getTemplate(templateDir + "/" + template + suffix);
            StringWriter out = new StringWriter();
            tpl.evaluate(out, data);
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render template: " + template, e);
        }
    }
}
