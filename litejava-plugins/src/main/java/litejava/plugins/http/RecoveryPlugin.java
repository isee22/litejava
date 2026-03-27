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
 * recovery.errorPageEnabled=false
 * recovery.errorPage.404=error/404
 * recovery.errorPage.500=error/500
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 默认使用 - 返回 JSON 错误
 * app.use(new RecoveryPlugin());
 * 
 * // 自定义错误处理
 * app.use(new RecoveryPlugin((ctx, e) -> {
 *     ctx.status(500).json(Map.of("error", e.getMessage()));
 *     logger.error("Request failed", e);
 * }));
 * 
 * // 开发模式 - 显示堆栈
 * app.use(RecoveryPlugin.withStack());
 * 
 * // 错误页面模式 - 需要配置模板引擎
 * RecoveryPlugin recovery = new RecoveryPlugin();
 * recovery.errorPageEnabled = true;
 * recovery.errorPages.put(404, "error/404");
 * recovery.errorPages.put(500, "error/500");
 * app.use(recovery);
 * }</pre>
 */
public class RecoveryPlugin extends MiddlewarePlugin {
    
    private final BiConsumer<Context, Throwable> errorHandler;
    public boolean showStack = false;
    public boolean errorPageEnabled = false;
    public Map<Integer, String> errorPages = new LinkedHashMap<>();
    
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
        errorPageEnabled = app.conf.getBool("recovery", "errorPageEnabled", errorPageEnabled);
        
        // 从配置文件读取错误页面路径
        String page404 = app.conf.get("recovery", "errorPage.404");
        String page500 = app.conf.get("recovery", "errorPage.500");
        if (page404 != null) errorPages.put(404, page404);
        if (page500 != null) errorPages.put(500, page500);
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
        int statusCode = 500;
        int code = 500;
        String msg = e.getMessage() != null ? e.getMessage() : "Internal Server Error";
        
        if (e instanceof LiteJavaException) {
            LiteJavaException le = (LiteJavaException) e;
            statusCode = le.statusCode;
            code = le.code;
        }
        
        ctx.status(statusCode);
        
        // 错误页面模式 - 渲染 HTML
        if (errorPageEnabled && isHtmlRequest(ctx)) {
            renderErrorPage(ctx, statusCode, code, msg, e, devMode);
            return;
        }
        
        // JSON 模式 - 返回 JSON
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
        logError(ctx, code, msg, e, devMode);
    }
    
    /**
     * 判断是否为 HTML 请求
     */
    private boolean isHtmlRequest(Context ctx) {
        String accept = ctx.header("Accept");
        if (accept == null) return false;
        return accept.contains("text/html");
    }
    
    /**
     * 渲染错误页面
     */
    private void renderErrorPage(Context ctx, int statusCode, int code, String msg, Throwable e, boolean devMode) {
        // 查找对应的错误页面模板
        String template = errorPages.get(statusCode);
        if (template == null) {
            // 5xx 用 500 页面，4xx 用 404 页面
            if (statusCode >= 500) {
                template = errorPages.get(500);
            } else if (statusCode >= 400) {
                template = errorPages.get(404);
            }
        }
        
        // 没有配置错误页面，提示用户
        if (template == null) {
            String error = "RecoveryPlugin: errorPageEnabled=true but no error page configured for status " + statusCode;
            if (ctx.app != null && ctx.app.log != null) {
                ctx.app.log.error(error);
            }
            // 降级为 JSON 响应
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", code);
            response.put("msg", msg);
            response.put("data", null);
            response.put("error", "Error page not configured. Please set recovery.errorPages or use JSON mode.");
            ctx.json(response);
            return;
        }
        
        // 检查是否有模板引擎
        if (ctx.app == null || ctx.app.view == null) {
            String error = "RecoveryPlugin: errorPageEnabled=true but no ViewPlugin configured. Please install ThymeleafPlugin or FreemarkerPlugin.";
            if (ctx.app != null && ctx.app.log != null) {
                ctx.app.log.error(error);
            }
            // 降级为 JSON 响应
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", code);
            response.put("msg", msg);
            response.put("data", null);
            response.put("error", "ViewPlugin not configured. Please install a template engine.");
            ctx.json(response);
            return;
        }
        
        // 渲染模板
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("statusCode", statusCode);
            data.put("code", code);
            data.put("message", msg);
            data.put("error", e);
            
            if (showStack || devMode) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                data.put("stack", sw.toString());
            }
            
            ctx.render(template, data);
        } catch (Exception renderError) {
            // 渲染失败，记录错误并降级为 JSON
            if (ctx.app != null && ctx.app.log != null) {
                ctx.app.log.error("Failed to render error page: " + template, renderError);
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", code);
            response.put("msg", msg);
            response.put("data", null);
            response.put("error", "Failed to render error page: " + renderError.getMessage());
            ctx.json(response);
        }
    }
    
    /**
     * 记录错误日志
     */
    private void logError(Context ctx, int code, String msg, Throwable e, boolean devMode) {
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
