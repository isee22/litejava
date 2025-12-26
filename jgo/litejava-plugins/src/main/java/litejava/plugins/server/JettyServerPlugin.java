package litejava.plugins.server;

import litejava.*;
import litejava.exception.LiteJavaException;
import litejava.plugin.ServerPlugin;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Jetty HTTP 服务器插件 - Jetty 11 (Jakarta Servlet)
 */
public class JettyServerPlugin extends ServerPlugin {
    
    public Server server;
    public int minThreads = 8;
    public int maxThreads = 200;
    public int idleTimeout = 60000;
    
    // 需要读取 body 的方法
    private static final String[] BODY_METHODS = {"POST", "PUT", "PATCH"};
    
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
        minThreads = app.conf.getInt("server.threads", "min", minThreads);
        maxThreads = app.conf.getInt("server.threads", "max", maxThreads);
        idleTimeout = app.conf.getInt("server", "idleTimeout", idleTimeout);
    }
    
    @Override
    public void start() {
        try {
            QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
            server = new Server(threadPool);
            
            org.eclipse.jetty.server.ServerConnector connector = 
                new org.eclipse.jetty.server.ServerConnector(server);
            connector.setHost(host);
            connector.setPort(app.port);
            server.addConnector(connector);
            
            server.setHandler(new JettyHandler());
            server.start();
            
            app.log.info("Jetty server started on " + host + ":" + app.port);
        } catch (Exception e) {
            throw new LiteJavaException("Failed to start Jetty server", e);
        }
    }
    
    @Override
    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    private class JettyHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, 
                          HttpServletRequest request, HttpServletResponse response) {
            Context ctx = acquireContext();
            
            try {
                parseRequest(request, ctx);
                app.handle(ctx);
            } catch (Exception e) {
                app.handleError(ctx, e);
            } finally {
                sendResponse(response, ctx);
                baseRequest.setHandled(true);
                releaseContext(ctx);
            }
        }
    }
    
    protected boolean needsBody(String method) {
        for (String m : BODY_METHODS) {
            if (m.equals(method)) return true;
        }
        return false;
    }
    
    protected void parseRequest(HttpServletRequest request, Context ctx) throws Exception {
        ctx.method = request.getMethod();
        ctx.path = request.getRequestURI();
        ctx.query = request.getQueryString();
        ctx.remoteAddr = request.getRemoteAddr();
        
        // 只有有 query string 时才解析参数
        if (ctx.query != null && !ctx.query.isEmpty()) {
            Map<String, String[]> params = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String[] v = entry.getValue();
                if (v.length > 0) ctx.queryParams.put(entry.getKey(), v[0]);
            }
        }
        
        // 只有 POST/PUT/PATCH 才读取 body
        if (needsBody(ctx.method)) {
            String contentType = request.getContentType();
            if (contentType != null) {
                ctx.headers.put("Content-Type", contentType);
            }
            
            int contentLength = request.getContentLength();
            if (contentLength > 0) {
                byte[] body = new byte[contentLength];
                InputStream is = request.getInputStream();
                int offset = 0;
                while (offset < contentLength) {
                    int read = is.read(body, offset, contentLength - offset);
                    if (read == -1) break;
                    offset += read;
                }
                ctx.setRequestBody(body);
            } else if (contentLength < 0) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
                InputStream is = request.getInputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                ctx.setRequestBody(bos.toByteArray());
            }
        }
    }
    
    protected void sendResponse(HttpServletResponse response, Context ctx) {
        try {
            response.setStatus(ctx.getResponseStatus());
            
            Map<String, String> headers = ctx.getResponseHeaders();
            if (!headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    response.setHeader(entry.getKey(), entry.getValue());
                }
            }
            
            byte[] body = ctx.getResponseBody();
            if (body.length > 0) {
                response.setContentLength(body.length);
                response.getOutputStream().write(body);
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
