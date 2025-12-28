package litejava.plugins.http;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 全局异常处理插件 - 统一捕获和处理所有异常
 * 
 * <h2>工作原理</h2>
 * <p>作为中间件包裹所有请求，捕获 handler 抛出的异常，返回统一格式的错误响应。
 * <p>框架内置的 404（路由不存在）和 405（方法不允许）也会抛出 LiteJavaException，
 * 由本插件统一处理，无需单独配置。
 * 
 * <h2>用户要做什么</h2>
 * <ol>
 *   <li><b>注册插件</b>：在 app.use() 中注册 ExceptionPlugin</li>
 *   <li><b>抛出异常</b>：业务代码中用 ctx.error() 或直接 throw 异常</li>
 *   <li><b>自动处理</b>：插件自动捕获异常，返回 JSON 错误响应</li>
 * </ol>
 * 
 * <h2>框架自动处理的异常</h2>
 * <ul>
 *   <li><b>404 Not Found</b> - 路由不存在时，App.handle() 自动抛出</li>
 *   <li><b>405 Method Not Allowed</b> - 路由存在但方法不匹配时，App.handle() 自动抛出</li>
 * </ul>
 * <p>这些异常都是 LiteJavaException，携带对应的 statusCode，由 ExceptionPlugin 统一处理。
 * 
 * <h2>响应格式</h2>
 * <p>默认返回 RESTful 风格响应，HTTP 状态码反映错误类型：
 * <pre>
 * HTTP/1.1 404 Not Found
 * {"error": "用户不存在"}
 * 
 * HTTP/1.1 400 Bad Request
 * {"error": "无效的用户ID"}
 * </pre>
 * 
 * <h2>完整示例</h2>
 * <pre>{@code
 * // 1. 注册插件（404/405 自动处理，无需额外配置）
 * app.use(new ExceptionPlugin());
 * 
 * // 2. 业务代码中抛出异常
 * app.get("/users/:id", ctx -> {
 *     long id = ctx.paramLong("id");
 *     
 *     // 参数校验 - 400 Bad Request
 *     if (id <= 0) {
 *         ctx.error(400, "无效的用户ID");
 *     }
 *     
 *     User user = userService.findById(id);
 *     
 *     // 资源不存在 - 404 Not Found
 *     if (user == null) {
 *         ctx.error(404, "用户不存在");
 *     }
 *     
 *     ctx.ok(user);
 * });
 * 
 * // 3. 自动返回错误响应（HTTP 状态码 + JSON）
 * // 请求: GET /users/0
 * // HTTP/1.1 400 Bad Request
 * // {"error": "无效的用户ID"}
 * 
 * // 请求: GET /not-exist (路由不存在)
 * // HTTP/1.1 404 Not Found
 * // {"error": "Route not found: GET /not-exist"}
 * }</pre>
 * 
 * <h2>常用 HTTP 状态码</h2>
 * <table>
 *   <tr><th>状态码</th><th>含义</th><th>使用场景</th></tr>
 *   <tr><td>400</td><td>Bad Request</td><td>参数错误、格式错误</td></tr>
 *   <tr><td>401</td><td>Unauthorized</td><td>未登录、token 无效</td></tr>
 *   <tr><td>403</td><td>Forbidden</td><td>无权限访问</td></tr>
 *   <tr><td>404</td><td>Not Found</td><td>资源不存在</td></tr>
 *   <tr><td>409</td><td>Conflict</td><td>资源冲突（如用户名已存在）</td></tr>
 *   <tr><td>422</td><td>Unprocessable</td><td>业务逻辑错误</td></tr>
 *   <tr><td>500</td><td>Server Error</td><td>服务器内部错误</td></tr>
 * </table>
 * 
 * <h2>自定义错误处理</h2>
 * 
 * <h3>方式一：传入自定义处理器（推荐）</h3>
 * <p>可以针对不同状态码做不同处理，或自定义响应格式：
 * <pre>{@code
 * // 国内风格：统一 200，用 code 区分
 * app.use(new ExceptionPlugin((ctx, e) -> {
 *     int status = 500;
 *     if (e instanceof LiteJavaException) {
 *         status = ((LiteJavaException) e).statusCode;
 *     }
 *     ctx.status(status).json(Maps.of("code", status, "msg", e.getMessage(), "data", null));
 * }));
 * }</pre>
 * 
 * <h3>方式二：继承并覆盖 handleError</h3>
 * <pre>{@code
 * public class MyExceptionPlugin extends ExceptionPlugin {
 *     @Override
 *     protected void handleError(Context ctx, Throwable e) {
 *         int status = (e instanceof LiteJavaException) 
 *             ? ((LiteJavaException) e).statusCode : 500;
 *         ctx.status(status).json(Maps.of(
 *             "error", e.getMessage(),
 *             "timestamp", System.currentTimeMillis()
 *         ));
 *     }
 * }
 * }</pre>
 * 
 * <h2>配置</h2>
 * <pre>
 * exception:
 *   showStack: false  # 是否显示堆栈（开发模式自动开启）
 * </pre>
 * <p>或代码设置：
 * <pre>{@code
 * ExceptionPlugin ep = new ExceptionPlugin();
 * ep.showStack = true;
 * app.use(ep);
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see Context#error(int, String) 中断请求
 * @see litejava.exception.LiteJavaException 框架异常基类
 */
public class ExceptionPlugin extends MiddlewarePlugin {
    
    private final BiConsumer<Context, Throwable> errorHandler;
    
    /** 是否显示堆栈信息（开发模式自动开启） */
    public boolean showStack = false;
    
    public ExceptionPlugin() {
        this.errorHandler = null;
    }
    
    /**
     * 自定义错误处理
     * @param errorHandler 错误处理器 (ctx, exception) -> {}
     */
    public ExceptionPlugin(BiConsumer<Context, Throwable> errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    @Override
    public void config() {
        showStack = app.conf.getBool("exception", "showStack", showStack);
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
     * 处理异常 - 子类可覆盖此方法完全自定义错误处理逻辑
     * @param ctx 请求上下文
     * @param e 捕获的异常
     */
    protected void handleError(Context ctx, Throwable e) {
        // 自定义处理器优先
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
        
        // 从 LiteJavaException 获取状态码，其他异常默认 500
        int statusCode = 500;
        if (e instanceof litejava.exception.LiteJavaException) {
            statusCode = ((litejava.exception.LiteJavaException) e).statusCode;
        }
        
        ctx.status(statusCode);
        
        // 构建响应 - RESTful 风格
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", e.getMessage() != null ? e.getMessage() : "Internal Server Error");
        
        // LiteJavaException 可能携带额外信息
        if (e instanceof litejava.exception.LiteJavaException) {
            Map<String, Object> details = ((litejava.exception.LiteJavaException) e).details;
            if (details != null && !details.isEmpty()) {
                response.put("details", details);
            }
        }
        
        // 开发模式显示堆栈
        if (showStack || devMode) {
            response.put("type", e.getClass().getName());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            response.put("stack", sw.toString());
        }
        
        ctx.json(response);
        
        // 5xx 错误打印日志
        if (statusCode >= 500 && ctx.app != null && ctx.app.log != null) {
            ctx.app.log.error("Exception: " + e.getMessage());
            if (showStack || devMode) {
                e.printStackTrace();
            }
        }
    }
}
