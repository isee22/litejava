package litejava.plugins.http;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip 压缩中间件 - 压缩响应体
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>{@code
 * gzip:
 *   enabled: true
 *   minSize: 1024      # 最小压缩大小（字节）
 *   level: 6           # 压缩级别 1-9
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用配置文件参数
 * app.use(new GzipPlugin());
 * 
 * // 或手动设置
 * GzipPlugin gzip = new GzipPlugin();
 * gzip.minSize = 512;
 * app.use(gzip);
 * }</pre>
 */
public class GzipPlugin extends MiddlewarePlugin {
    
    // 默认配置
    public boolean enabled = true;
    public int minSize = 1024;  // 1KB
    public int level = 6;       // 压缩级别 1-9
    
    @Override
    public void config() {
        enabled = app.conf.getBool("gzip", "enabled", enabled);
        minSize = app.conf.getInt("gzip", "minSize", minSize);
        level = app.conf.getInt("gzip", "level", level);
        
        // 限制压缩级别范围
        if (level < 1) level = 1;
        if (level > 9) level = 9;
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        next.run();
        
        if (!enabled) return;
        
        String acceptEncoding = ctx.headers.get("Accept-Encoding");
        if (acceptEncoding == null || !acceptEncoding.contains("gzip")) {
            return;
        }
        
        byte[] body = ctx.getResponseBody();
        if (body.length < minSize) {
            return;
        }
        
        byte[] compressed = compress(body);
        if (compressed.length < body.length) {
            ctx.header("Content-Encoding", "gzip");
            ctx.data(compressed);
        }
    }
    
    private byte[] compress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos) {{
            def.setLevel(level);
        }}) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }
}
