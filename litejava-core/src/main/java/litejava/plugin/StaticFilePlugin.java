package litejava.plugin;

import litejava.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 静态文件目录插件 - Gin-style 路由方式提供静态资源
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 方式一：便捷方法（推荐）
 * app.staticDir("/static", "static");
 * 
 * // 方式二：插件方式（可自定义配置）
 * StaticFilePlugin plugin = new StaticFilePlugin("/static", "static");
 * plugin.cacheMaxAge = 86400;
 * plugin.indexFile = "home.html";
 * app.use(plugin);
 * 
 * // 方式三：继承扩展
 * class MyStaticPlugin extends StaticFilePlugin {
 *     public MyStaticPlugin() { super("/static", "static"); }
 *     @Override
 *     protected void beforeServe(Context ctx, String filepath) {
 *         ctx.header("X-Custom", "value");
 *     }
 * }
 * app.use(new MyStaticPlugin());
 * }</pre>
 * 
 * @see SingleFilePlugin 单文件插件
 */
public class StaticFilePlugin extends Plugin {
    
    public String urlPrefix;
    public String directory;
    public String indexFile = "index.html";
    public int cacheMaxAge = 3600;
    public boolean useFileSystem = false;
    
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("htm", "text/html; charset=utf-8");
        MIME_TYPES.put("css", "text/css; charset=utf-8");
        MIME_TYPES.put("js", "application/javascript; charset=utf-8");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("xml", "application/xml");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("zip", "application/zip");
        MIME_TYPES.put("mp4", "video/mp4");
        MIME_TYPES.put("webm", "video/webm");
        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("wav", "audio/wav");
    }
    
    /**
     * 默认构造函数（用于配置文件自动配置）
     * <p>需要通过字段设置 urlPrefix 和 directory
     */
    public StaticFilePlugin() {
        this.urlPrefix = "/static";
        this.directory = "static/";
    }
    
    public StaticFilePlugin(String urlPrefix, String directory) {
        this.urlPrefix = urlPrefix;
        this.directory = directory.endsWith("/") ? directory : directory + "/";
        this.useFileSystem = new File(directory).exists();
    }
    
    public StaticFilePlugin(String urlPrefix, String directory, boolean useFileSystem) {
        this.urlPrefix = urlPrefix;
        this.directory = directory.endsWith("/") ? directory : directory + "/";
        this.useFileSystem = useFileSystem;
    }
    
    @Override
    public void config() {
        cacheMaxAge = app.conf.getInt("static", "cacheMaxAge", cacheMaxAge);
        indexFile = app.conf.getString("static", "indexFile", indexFile);
        
        // 注册通配符路由（Gin-style）
        String routePath = urlPrefix.endsWith("/") 
            ? urlPrefix + "*filepath" 
            : urlPrefix + "/*filepath";
        
        app.get(routePath, this::handleStatic);
        
        // 同时注册不带通配符的路径（访问目录根）
        String basePath = urlPrefix.endsWith("/") 
            ? urlPrefix.substring(0, urlPrefix.length() - 1) 
            : urlPrefix;
        app.get(basePath, this::handleIndex);
    }
    
    /**
     * 处理静态文件请求
     */
    private void handleStatic(Context ctx) throws Exception {
        String filepath = ctx.pathParam("filepath");
        if (filepath == null || filepath.isEmpty()) {
            filepath = indexFile;
        }
        
        serveFile(ctx, filepath);
    }
    
    /**
     * 处理目录根请求
     */
    private void handleIndex(Context ctx) throws Exception {
        serveFile(ctx, indexFile);
    }
    
    /**
     * 提供文件服务（protected 允许子类重写）
     */
    protected void serveFile(Context ctx, String filepath) throws Exception {
        // 防止路径遍历攻击
        if (filepath.contains("..")) {
            ctx.status(403).text("Forbidden");
            return;
        }
        
        byte[] content;
        if (useFileSystem) {
            content = readFromFileSystem(filepath);
        } else {
            content = readFromClasspath(filepath);
        }
        
        if (content == null) {
            ctx.status(404).text("Not Found");
            return;
        }
        
        String ext = getExtension(filepath);
        String mimeType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        
        ctx.status(200)
           .header("Cache-Control", "public, max-age=" + cacheMaxAge)
           .data(content, mimeType);
    }
    
    private byte[] readFromFileSystem(String relativePath) {
        try {
            Path filePath = Paths.get(directory, relativePath).normalize();
            // 确保路径在目录内
            if (!filePath.startsWith(Paths.get(directory).normalize())) {
                return null;
            }
            File file = filePath.toFile();
            if (file.exists() && file.isFile()) {
                return Files.readAllBytes(file.toPath());
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
    
    private byte[] readFromClasspath(String relativePath) {
        String resourcePath = directory + relativePath;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
    
    /**
     * 获取 MIME 类型
     */
    public static String getMimeType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
