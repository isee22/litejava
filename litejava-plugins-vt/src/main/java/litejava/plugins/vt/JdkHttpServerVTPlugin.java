package litejava.plugins.vt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import litejava.Context;
import litejava.exception.LiteJavaException;
import litejava.plugin.ServerPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * JDK 内置 HttpServer + 虚拟线程 (Java 21+)
 * 最轻量的 Java HTTP 方案，无任何框架开销
 */
public class JdkHttpServerVTPlugin extends ServerPlugin {
    
    private HttpServer server;
    private final ObjectPool<Context> contextPool = new ObjectPool<>(
        Context::new,
        ctx -> {
            ctx.app = null;
            ctx.method = null;
            ctx.path = null;
            ctx.query = null;
            ctx.remoteAddr = null;
            ctx.wildcardPath = null;
            ctx.headers.clear();
            ctx.params.clear();
            ctx.queryParams.clear();
            ctx.state.clear();
        }
    );
    private static final String[] BODY_METHODS = {"POST", "PUT", "PATCH"};
    
    @Override
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(host, app.port), backlog);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.createContext("/", this::handleRequest);
            server.start();
            app.log.info("JDK HttpServer (Virtual Threads) started on " + host + ":" + app.port);
        } catch (IOException e) {
            throw new LiteJavaException("Failed to start JDK HttpServer", e);
        }
    }
    
    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
    
    private void handleRequest(HttpExchange exchange) {
        Context ctx = contextPool.acquire();
        ctx.app = app;
        
        try {
            parseRequest(exchange, ctx);
            app.handle(ctx);
        } catch (Exception e) {
            app.handleError(ctx, e);
        } finally {
            sendResponse(exchange, ctx);
            contextPool.release(ctx);
        }
    }
    
    private void parseRequest(HttpExchange exchange, Context ctx) throws IOException {
        ctx.method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        ctx.path = uri.getPath();
        ctx.query = uri.getQuery();
        ctx.remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();
        
        // 解析 query 参数
        if (ctx.query != null && !ctx.query.isEmpty()) {
            parseQueryParams(ctx.query, ctx.queryParams);
        }
        
        // 只有 POST/PUT/PATCH 才读取 body
        if (needsBody(ctx.method)) {
            // 读取 Content-Type
            List<String> ctList = exchange.getRequestHeaders().get("Content-Type");
            if (ctList != null && !ctList.isEmpty()) {
                ctx.headers.put("Content-Type", ctList.get(0));
            }
            
            // 读取 body
            try (InputStream is = exchange.getRequestBody()) {
                ctx.setRequestBody(is.readAllBytes());
            }
        }
    }
    
    private void parseQueryParams(String query, Map<String, String> params) {
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
                params.put(key, value);
            }
        }
    }
    
    private boolean needsBody(String method) {
        for (String m : BODY_METHODS) {
            if (m.equals(method)) return true;
        }
        return false;
    }
    
    private void sendResponse(HttpExchange exchange, Context ctx) {
        try {
            // 设置响应头
            Map<String, String> headers = ctx.getResponseHeaders();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
            }
            
            byte[] body = ctx.getResponseBody();
            exchange.sendResponseHeaders(ctx.getResponseStatus(), body.length);
            
            if (body.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            } else {
                exchange.getResponseBody().close();
            }
        } catch (IOException e) {
            // ignore
        } finally {
            exchange.close();
        }
    }
}
