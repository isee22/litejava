package litejava.plugin;

import litejava.*;
import litejava.util.Files;

import java.util.*;

/**
 * 静态文件插件 - Gin 风格 URL-目录映射
 * 
 * <h2>代码配置（Gin 风格）</h2>
 * <pre>{@code
 * app.use(new StaticFilePlugin()
 *     .add("/static", "static/")
 *     .add("/uploads", "/data/uploads/"));
 * }</pre>
 * 
 * <h2>配置文件 - YAML</h2>
 * <pre>
 * static:
 *   mappings:
 *     /static: static/
 *     /uploads: /data/uploads/
 *   cache-max-age: 3600
 *   index-file: index.html
 * </pre>
 * 
 * <h2>配置文件 - Properties</h2>
 * <pre>
 * static.mappings./static=static/
 * static.mappings./uploads=/data/uploads/
 * static.cache-max-age=3600
 * static.index-file=index.html
 * </pre>
 * 
 * <h2>配置项说明</h2>
 * <table border="1">
 *   <tr><th>配置项</th><th>类型</th><th>默认值</th><th>说明</th></tr>
 *   <tr><td>static.mappings</td><td>Map</td><td>-</td><td>URL前缀 → 目录 映射</td></tr>
 *   <tr><td>static.cache-max-age</td><td>int</td><td>3600</td><td>浏览器缓存时间（秒）</td></tr>
 *   <tr><td>static.index-file</td><td>String</td><td>index.html</td><td>目录默认首页文件</td></tr>
 * </table>
 * 
 * <h2>路径格式示例</h2>
 * <pre>
 * # 相对路径（自动检测：先文件系统，没有再 classpath）
 * /static: static/
 * 
 * # Linux/Mac 绝对路径 → 文件系统
 * /uploads: /data/uploads/
 * 
 * # Windows 绝对路径 → 文件系统
 * /uploads: D:/git/project/uploads/
 * 
 * # 强制 classpath（JAR 包内资源）
 * /assets: classpath:assets/
 * 
 * # 强制文件系统
 * /files: file:/var/www/files/
 * </pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 */
public class StaticFilePlugin extends Plugin {
    
    /** URL前缀 -> 目录 映射 */
    public Map<String, String> mappings = new LinkedHashMap<>();
    
    /** 默认首页文件 */
    public String indexFile = "index.html";
    
    /** 缓存时间（秒） */
    public int cacheMaxAge = 3600;
    
    public StaticFilePlugin() {}
    
    /**
     * 添加 URL-目录映射（Gin 风格）
     * @param urlPrefix URL 前缀，如 "/static"
     * @param directory 目录路径，如 "static/" 或 "file:/data/uploads/"
     */
    public StaticFilePlugin add(String urlPrefix, String directory) {
        mappings.put(urlPrefix, directory);
        return this;
    }
    
    @Override
    public void config() {
        // 从配置文件加载（kebab-case）
        cacheMaxAge = app.conf.getInt("static", "cache-max-age", cacheMaxAge);
        indexFile = app.conf.getString("static", "index-file", indexFile);
        
        // 从配置文件加载 mappings
        Map<String, Object> confMappings = app.conf.get("static.mappings");
        if (confMappings != null) {
            for (Map.Entry<String, Object> entry : confMappings.entrySet()) {
                String urlPrefix = entry.getKey();
                String directory = String.valueOf(entry.getValue());
                if (!mappings.containsKey(urlPrefix)) {
                    mappings.put(urlPrefix, directory);
                }
            }
        }
        
        // 注册路由
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String urlPrefix = entry.getKey();
            String directory = entry.getValue();
            registerRoutes(urlPrefix, directory);
        }
    }
    
    /**
     * 注册静态文件路由
     */
    public void registerRoutes(String urlPrefix, String directory) {
        String dir = directory.endsWith("/") ? directory : directory + "/";
        
        // 注册通配符路由
        String routePath = urlPrefix.endsWith("/") 
            ? urlPrefix + "*filepath" 
            : urlPrefix + "/*filepath";
        
        app.get(routePath, ctx -> serveFile(ctx, dir, ctx.pathParam("filepath")));
        
        // 注册目录根路由（返回 index.html）
        String basePath = urlPrefix.endsWith("/") 
            ? urlPrefix.substring(0, urlPrefix.length() - 1) 
            : urlPrefix;
        app.get(basePath, ctx -> serveFile(ctx, dir, indexFile));
    }
    
    /**
     * 提供文件服务
     */
    public void serveFile(Context ctx, String directory, String filepath) throws Exception {
        if (filepath == null || filepath.isEmpty()) {
            filepath = indexFile;
        }
        
        // 防止路径遍历攻击
        if (filepath.contains("..")) {
            ctx.status(403).text("Forbidden");
            return;
        }
        
        byte[] content = Files.read(directory, filepath);
        
        if (content == null) {
            ctx.status(404).text("Not Found");
            return;
        }
        
        String mimeType = Files.getMimeType(filepath);
        
        ctx.status(200)
           .header("Cache-Control", "public, max-age=" + cacheMaxAge)
           .data(content, mimeType);
    }
}
