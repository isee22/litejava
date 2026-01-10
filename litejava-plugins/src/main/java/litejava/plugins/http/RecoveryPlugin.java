package litejava.plugins.http;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;
import litejava.exception.LiteJavaException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Recovery 中间件 - 捕获异常防止服务崩溃 (Gin-style)
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * recovery.showStack=false
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 默认使用 - 返回 500 错误
 * app.use(new RecoveryPlugin());
 * 
 * // 自定义错误处理
 * app.use(new RecoveryPlugin((ctx, e) -> {
 *     ctx.status(500).json(Map.of("error", e.getMessage()));
 *     // 记录日志
 *     logger.error("Request failed", e);
 * }));
 * 
 * // 开发模式 - 显示堆栈
 * app.use(RecoveryPlugin.withStack());
 * }</pre>
 */
public class RecoveryPlugin extends MiddlewarePlugin {
    
    private final BiConsumer<Context, Throwable> errorHandler;
    public boolean showStack = false;
    
    public RecoveryPlugin() {
        this.errorHandler = null;
    }
    
    public RecoveryPlugin(BiConsumer<Context, Throwable> errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    /**
     * 创建显示堆栈的 Recovery (开发模式)
     */
    public static RecoveryPlugin withStack() {
        RecoveryPlugin plugin = new RecoveryPlugin();
        plugin.showStack = true;
        return plugin;
    }
    
    @Override
    public void config() {
        showStack = app.conf.getBool("recovery", "showStack", showStack);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        try {
            next.run();
        } catch (Throwable e) {
            handleError(ctx, e);
        }
    }
    
    private void handleError(Context ctx, Throwable e) {
        // 自定义处理器
        if (errorHandler != null) {
            try {
                errorHandler.accept(ctx, e);
                return;
            } catch (Exception handlerError) {
                e = handlerError;
            }
        }
        
        // 自动检测 devMode
        boolean devMode = ctx.app != null && ctx.app.devMode;
        
        // 业务异常 vs 系统异常
        int code = 500;
        String msg = e.getMessage() != null ? e.getMessage() : "Internal Server Error";
        
        if (e instanceof LiteJavaException) {
            LiteJavaException le = (LiteJavaException) e;
            ctx.status(le.statusCode);
            code = le.code;
        } else {
            ctx.status(500);
        }
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", code);
        response.put("msg", msg);
        response.put("data", null);
        
        if (showStack || devMode) {
            response.put("type", e.getClass().getName());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            response.put("stack", sw.toString());
        }
        
        ctx.json(response);
        
        // 打印到控制台 (业务异常用 warn，系统异常用 error)
        if (ctx.app != null && ctx.app.log != null) {
            if (code >= 500 || code < 100) {
                ctx.app.log.error("Recovery caught exception: " + e.getMessage());
                if (showStack || devMode) {
                    e.printStackTrace();
                }
            } else {
                ctx.app.log.warn("Business exception: [" + code + "] " + msg);
            }
        }
    }
}
