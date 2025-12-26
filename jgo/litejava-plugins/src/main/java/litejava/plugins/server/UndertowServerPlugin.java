package litejava.plugins.server;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import litejava.*;
import litejava.exception.LiteJavaException;
import litejava.plugin.ServerPlugin;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Undertow HTTP 服务器插件 - 轻量高性能服务器
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.undertow</groupId>
 *     <artifactId>undertow-core</artifactId>
 *     <version>2.3.10.Final</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * server.port=8080
 * server.host=0.0.0.0
 * server.threads.io=0           # 0 = CPU cores
 * server.threads.worker=0       # 0 = CPU cores * 8
 * server.maxRequestSize=10485760
 * }</pre>
 * 
 * <h2>使用</h2>
 * <pre>{@code
 * app.use(new UndertowServerPlugin());
 * }</pre>
 */
public class UndertowServerPlugin extends ServerPlugin {
    
    public Undertow server;
    
    // Undertow 特有配置
    public int ioThreads = 0;      // 0 = Undertow 默认 (CPU cores)
    public int workerThreads = 0;  // 0 = Undertow 默认 (CPU cores * 8)
    
    // Context 对象池
    private final ConcurrentLinkedQueue<Context> contextPool = new ConcurrentLinkedQueue<>();
    private static final int POOL_MAX_SIZE = 1024;
    
    protected Context acquireContext() {
        Context ctx = contextPool.poll();
        if (ctx == null) {
            ctx = new Context();
        }
        ctx.app = app;
        return ctx;
    }
    
    protected void releaseContext(Context ctx) {
        if (ctx == null || contextPool.size() >= POOL_MAX_SIZE) return;
        ctx.reset();
        contextPool.offer(ctx);
    }
    
    @Override
    public void config() {
        super.config();
        ioThreads = app.conf.getInt("server.threads", "io", ioThreads);
        workerThreads = app.conf.getInt("server.threads", "worker", workerThreads);
    }
    
    @Override
    public void start() {
        try {
            Undertow.Builder builder = Undertow.builder()
                .addHttpListener(app.port, host)
                .setHandler(this::handleRequest);
            
            if (ioThreads > 0) {
                builder.setIoThreads(ioThreads);
            }
            if (workerThreads > 0) {
                builder.setWorkerThreads(workerThreads);
            }
            
            server = builder.build();
            server.start();
            app.log.info("Undertow server started on " + host + ":" + app.port);
        } catch (Exception e) {
            throw new LiteJavaException("Failed to start Undertow server", e);
        }
    }
    
    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
    
    private void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this::handleRequest);
            return;
        }
        
        exchange.getRequestReceiver().receiveFullBytes((ex, body) -> {
            Context ctx = acquireContext();
            
            try {
                parseRequest(ex, body, ctx);
                app.handle(ctx);
            } catch (Exception e) {
                app.handleError(ctx, e);
            } finally {
                sendResponse(ex, ctx);
                releaseContext(ctx);
            }
        });
    }
    
    /**
     * 解析 HTTP 请求
     */
    protected void parseRequest(HttpServerExchange exchange, byte[] body, Context ctx) {
        ctx.method = exchange.getRequestMethod().toString();
        ctx.path = exchange.getRequestPath();
        ctx.query = exchange.getQueryString();
        
        HeaderMap headers = exchange.getRequestHeaders();
        headers.getHeaderNames().forEach(name -> {
            ctx.headers.put(name.toString(), headers.getFirst(name));
        });
        
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        params.forEach((k, v) -> {
            if (!v.isEmpty()) ctx.queryParams.put(k, v.getFirst());
        });
        
        ctx.setRequestBody(body);
    }
    
    /**
     * 发送 HTTP 响应
     */
    protected void sendResponse(HttpServerExchange exchange, Context ctx) {
        exchange.setStatusCode(ctx.getResponseStatus());
        
        for (Map.Entry<String, String> entry : ctx.getResponseHeaders().entrySet()) {
            exchange.getResponseHeaders().put(new HttpString(entry.getKey()), entry.getValue());
        }
        
        byte[] body = ctx.getResponseBody();
        exchange.getResponseSender().send(ByteBuffer.wrap(body));
    }
}
