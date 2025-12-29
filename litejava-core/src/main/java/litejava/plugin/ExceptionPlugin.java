package litejava.plugin;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.util.function.BiConsumer;

/**
 * 全局异常处理插件 - 统一捕获和处理所有异常
 * 
 * <p>App 内置此插件，默认在中间件最前面，无需手动添加。
 * 
 * <h2>使用方式</h2>
 * 
 * <h3>1. 使用默认行为</h3>
 * <pre>{@code
 * // 默认返回 JSON: {"error": "错误信息"}
 * App app = new App();
 * app.run();
 * }</pre>
 * 
 * <h3>2. 自定义处理器</h3>
 * <pre>{@code
 * app.exception.handler = (ctx, e) -> {
 *     int status = e instanceof LiteJavaException ? ((LiteJavaException) e).statusCode : 500;
 *     
 *     if (status == 404) {
 *         ctx.status(404).text("Not Found");
 *     } else if (status == 405) {
 *         ctx.status(405).text("Method Not Allowed");
 *     } else {
 *         ctx.status(500).json(Maps.of("error", e.getMessage()));
 *     }
 * };
 * }</pre>
 */
public class ExceptionPlugin extends MiddlewarePlugin {
    
    /** 错误处理器 */
    public BiConsumer<Context, Throwable> handler;
    
    public ExceptionPlugin() {
    }
    
    public ExceptionPlugin(BiConsumer<Context, Throwable> handler) {
        this.handler = handler;
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        try {
            next.run();
        } catch (Throwable e) {
            handleError(ctx, e);
        }
    }
    
    /**
     * 处理异常（可重写自定义）
     */
    public void handleError(Context ctx, Throwable e) {
        // 用户自定义处理
        if (handler != null) {
            try {
                handler.accept(ctx, e);
                return;
            } catch (Exception handlerError) {
                e = handlerError;
            }
        }
        
        // 默认处理：根据 statusCode 返回对应状态码和错误信息
        int status = 500;
        if (e instanceof litejava.exception.LiteJavaException) {
            status = ((litejava.exception.LiteJavaException) e).statusCode;
        }
        
        String message = e.getMessage() != null ? e.getMessage() : "Internal Server Error";
        ctx.status(status).json(java.util.Collections.singletonMap("error", message));
    }
}
