package litejava.plugin;

import litejava.App;
import litejava.Context;
import litejava.Handler;
import litejava.MiddlewareChain;
import litejava.Plugin;
import litejava.exception.LiteJavaException;

/**
 * 请求处理插件 - 负责路由匹配和中间件执行
 * 
 * <p>App 内置此插件，处理所有 HTTP 请求。
 * 
 * <h2>处理流程</h2>
 * <ol>
 *   <li>执行中间件链（洋葱模型）</li>
 *   <li>路由匹配</li>
 *   <li>提取路径参数</li>
 *   <li>执行 handler</li>
 * </ol>
 * 
 * <h2>自定义处理流程</h2>
 * <pre>{@code
 * public class MyHandlerPlugin extends HandlerPlugin {
 *     @Override
 *     public void handle(Context ctx) throws Exception {
 *         long start = System.currentTimeMillis();
 *         try {
 *             super.handle(ctx);
 *         } finally {
 *             System.out.println(ctx.method + " " + ctx.path + " " + (System.currentTimeMillis() - start) + "ms");
 *         }
 *     }
 * }
 * 
 * // 替换内置插件
 * app.use(new MyHandlerPlugin());
 * }</pre>
 */
public class HandlerPlugin extends Plugin {
    
    @Override
    public void config() {
        // 无配置
    }
    
    /**
     * 处理 HTTP 请求
     * 
     * @param ctx 请求上下文
     * @throws Exception 处理异常
     */
    public void handle(Context ctx) throws Exception {
        // 路由匹配和执行作为最终 handler
        Handler routeHandler = c -> {
            RouterPlugin.RouteMatch match = app.router.match(c.method, c.path);
            
            if (match != null) {
                if (match.params != null && !match.params.isEmpty()) {
                    c.params = match.params;
                }
                if (match.wildcardName != null) {
                    c.wildcardPath = match.wildcardValue;
                    c.params.put(match.wildcardName, match.wildcardValue);
                }
                match.handler.handle(c);
            } else if (app.router.hasPath(c.path)) {
                throw new LiteJavaException("Method Not Allowed: " + c.method + " " + c.path, 405);
            } else {
                throw new LiteJavaException("Not Found: " + c.path, 404);
            }
        };
        
        // 无中间件时直接执行路由
        if (app.middlewares.isEmpty()) {
            routeHandler.handle(ctx);
        } else {
            new MiddlewareChain(app.middlewares, routeHandler).execute(ctx);
        }
    }
}
