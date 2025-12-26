package litejava.plugins.vt;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import litejava.Context;
import litejava.exception.LiteJavaException;
import litejava.plugins.server.UndertowServerPlugin;

/**
 * Undertow HTTP 服务器插件 - 虚拟线程版本 (Java 21+)
 * 继承 UndertowServerPlugin，使用虚拟线程处理请求
 */
public class UndertowVirtualThreadPlugin extends UndertowServerPlugin {
    
    @Override
    public void start() {
        try {
            Undertow.Builder builder = Undertow.builder()
                .addHttpListener(app.port, host)
                .setHandler(this::handleRequestWithVirtualThread);
            
            if (ioThreads > 0) {
                builder.setIoThreads(ioThreads);
            }
            
            // 不设置 workerThreads，使用虚拟线程代替
            builder.setWorkerThreads(1);
            
            server = builder.build();
            server.start();
            app.log.info("Undertow (Virtual Threads) started on " + host + ":" + app.port);
        } catch (Exception e) {
            throw new LiteJavaException("Failed to start Undertow server", e);
        }
    }
    
    private void handleRequestWithVirtualThread(HttpServerExchange exchange) {
        // 使用虚拟线程处理请求
        Thread.startVirtualThread(() -> {
            exchange.getRequestReceiver().receiveFullBytes((ex, body) -> {
                Context ctx = new Context();
                ctx.app = app;
                
                try {
                    parseRequest(ex, body, ctx);
                    app.handle(ctx);
                } catch (Exception e) {
                    app.handleError(ctx, e);
                }
                
                sendResponse(ex, ctx);
            });
        });
    }
}
