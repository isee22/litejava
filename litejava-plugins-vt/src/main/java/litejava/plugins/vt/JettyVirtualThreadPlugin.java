package litejava.plugins.vt;

import litejava.Context;
import litejava.exception.LiteJavaException;
import litejava.plugins.server.JettyServerPlugin;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Jetty HTTP 服务器插件 - 虚拟线程版本 (Java 21+)
 * 继承 JettyServerPlugin，使用虚拟线程处理请求
 */
public class JettyVirtualThreadPlugin extends JettyServerPlugin {
    
    @Override
    public void start() {
        try {
            // 直接使用虚拟线程执行器，不需要 QueuedThreadPool 的队列管理
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            
            server = new org.eclipse.jetty.server.Server();
            
            org.eclipse.jetty.server.ServerConnector connector = 
                new org.eclipse.jetty.server.ServerConnector(server);
            connector.setHost(host);
            connector.setPort(app.port);
            server.addConnector(connector);
            
            // 使用虚拟线程执行器
            server.setHandler(new VirtualThreadHandler());
            
            // 设置 Jetty 使用虚拟线程
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setVirtualThreadsExecutor(executor);
            server.addBean(threadPool);
            
            server.start();
            
            app.log.info("Jetty (Virtual Threads) started on " + host + ":" + app.port);
        } catch (Exception e) {
            throw new LiteJavaException("Failed to start Jetty server", e);
        }
    }
    
    private class VirtualThreadHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, 
                          HttpServletRequest request, HttpServletResponse response) {
            Context ctx = new Context();
            ctx.app = app;
            
            try {
                parseRequest(request, ctx);
                app.handle(ctx);
            } catch (Exception e) {
                app.handleError(ctx, e);
            }
            
            sendResponse(response, ctx);
            baseRequest.setHandled(true);
        }
    }
}
