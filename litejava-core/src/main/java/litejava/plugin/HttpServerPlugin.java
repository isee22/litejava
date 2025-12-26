package litejava.plugin;

import com.sun.net.httpserver.*;

import litejava.Context;
import litejava.exception.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * HTTP 服务器插件 - 基于 JDK 内置 HttpServer（零外部依赖）
 * 
 * <p>这是 LiteJava 的默认服务器实现，特点：
 * <ul>
 *   <li>零外部依赖，纯 JDK 实现</li>
 *   <li>内置 Context 对象池，减少 GC 压力</li>
 *   <li>可配置线程池大小</li>
 *   <li>自动处理 Keep-Alive</li>
 * </ul>
 * 
 * <h2>配置项</h2>
 * <pre>
 * server.port=8080           # 服务器端口
 * server.host=0.0.0.0        # 绑定地址
 * server.threads.min=8       # 最小线程数
 * server.threads.max=200     # 最大线程数
 * server.backlog=1024        # 连接队列大小
 * server.idleTimeout=60      # 空闲超时（秒）
 * </pre>
 * 
 * <h2>使用方式</h2>
 * <pre>{@code
 * // 方式一：通过 LiteJava.create() 自动注册
 * App app = LiteJava.create();  // 默认使用 HttpServerPlugin
 * 
 * // 方式二：手动注册
 * App app = new App();
 * app.use(new HttpServerPlugin());
 * }</pre>
 * 
 * <h2>性能优化</h2>
 * <p>内置 Context 对象池，避免每次请求创建新对象：
 * <ul>
 *   <li>池大小：1024</li>
 *   <li>请求开始时从池获取 Context</li>
 *   <li>请求结束后重置并归还</li>
 * </ul>
 * 
 * <h2>替代方案</h2>
 * <p>如需更高性能，可使用其他服务器插件：
 * <ul>
 *   <li>JdkHttpServerVTPlugin - JDK 21+ 虚拟线程版本</li>
 *   <li>NettyServerPlugin - Netty 异步服务器</li>
 *   <li>JettyServerPlugin - Jetty 服务器</li>
 *   <li>UndertowServerPlugin - Undertow 服务器</li>
 * </ul>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see ServerPlugin 服务器插件基类
 */
public class HttpServerPlugin extends ServerPlugin {
    
    /** JDK HttpServer 实例 */
    public HttpServer server;
    
    /** 请求处理线程池 */
    public ExecutorService executor;
    
    // ==================== Context 对象池 ====================
    
    /** 对象池，减少 GC 压力 */
    private final ConcurrentLinkedQueue<Context> contextPool = new ConcurrentLinkedQueue<>();
    
    /** 对象池最大容量 */
    private static final int POOL_MAX_SIZE = 1024;
    
    /**
     * 从对象池获取 Context
     * @return Context 实例
     */
    protected Context acquireContext() {
        Context ctx = contextPool.poll();
        if (ctx == null) {
            ctx = new Context();
        }
        ctx.app = app;
        return ctx;
    }
    
    /**
     * 归还 Context 到对象池
     * @param ctx Context 实例
     */
    protected void releaseContext(Context ctx) {
        if (ctx == null || contextPool.size() >= POOL_MAX_SIZE) return;
        ctx.reset();
        contextPool.offer(ctx);
    }
    
    // ==================== 生命周期 ====================
    
    @Override
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(host, app.port), backlog);
            server.createContext("/", this::handleRequest);
            
            // 使用配置的线程池
            executor = new ThreadPoolExecutor(
                minThreads, maxThreads,
                idleTimeout, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
            );
            server.setExecutor(executor);
            server.start();
        } catch (IOException e) {
            throw new LiteJavaException("Failed to start server on port " + app.port, e);
        }
    }
    
    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    // ==================== 请求处理 ====================
    
    /**
     * 处理 HTTP 请求
     */
    private void handleRequest(HttpExchange exchange) {
        Context ctx = acquireContext();
        
        try {
            parseRequest(exchange, ctx);
            app.handle(ctx);
        } catch (Exception e) {
            app.handleError(ctx, e);
        } finally {
            sendResponse(exchange, ctx);
            releaseContext(ctx);
        }
    }
    
    /**
     * 解析 HTTP 请求
     */
    private void parseRequest(HttpExchange exchange, Context ctx) throws IOException {
        ctx.method = exchange.getRequestMethod();
        
        URI uri = exchange.getRequestURI();
        ctx.path = uri.getPath();
        ctx.query = uri.getQuery();
        
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                ctx.headers.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        
        if (ctx.query != null && !ctx.query.isEmpty()) {
            for (String pair : ctx.query.split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    try {
                        String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                        String value = idx < pair.length() - 1 ? 
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
                        ctx.queryParams.put(key, value);
                    } catch (UnsupportedEncodingException e) {
                        // UTF-8 always supported
                    }
                }
            }
        }
        
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        ctx.setRequestBody(baos.toByteArray());
    }
    
    /**
     * 发送 HTTP 响应
     */
    private void sendResponse(HttpExchange exchange, Context ctx) {
        try {
            Headers responseHeaders = exchange.getResponseHeaders();
            for (Map.Entry<String, String> entry : ctx.getResponseHeaders().entrySet()) {
                responseHeaders.set(entry.getKey(), entry.getValue());
            }
            
            byte[] body = ctx.getResponseBody();
            exchange.sendResponseHeaders(ctx.getResponseStatus(), body.length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(body);
            os.close();
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }
}
