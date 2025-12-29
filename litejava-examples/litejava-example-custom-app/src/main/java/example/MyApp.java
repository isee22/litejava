package example;

import litejava.App;
import litejava.Context;
import litejava.plugin.ExceptionPlugin;
import litejava.plugin.HandlerPlugin;
import litejava.util.Maps;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义 App 示例 - 展示如何通过插件自定义行为
 * 
 * <p>LiteJava 采用纯插件架构，自定义行为通过继承插件实现：
 * <ul>
 *   <li>{@link HandlerPlugin} - 自定义请求处理流程</li>
 *   <li>{@link ExceptionPlugin} - 自定义错误处理</li>
 * </ul>
 * 
 * <p>本示例演示：
 * <ul>
 *   <li>请求耗时日志（自定义 HandlerPlugin）</li>
 *   <li>统一错误响应格式（自定义 ExceptionPlugin）</li>
 * </ul>
 */
public class MyApp {
    
    /**
     * 自定义请求处理插件 - 添加耗时日志
     */
    public static class MyHandlerPlugin extends HandlerPlugin {
        @Override
        public void handle(Context ctx) throws Exception {
            long start = System.currentTimeMillis();
            try {
                super.handle(ctx);
            } finally {
                long cost = System.currentTimeMillis() - start;
                app.log.info(ctx.method + " " + ctx.path + " " + ctx.getResponseStatus() + " " + cost + "ms");
            }
        }
    }
    
    /**
     * 自定义异常处理插件 - 统一错误响应格式
     */
    public static class MyExceptionPlugin extends ExceptionPlugin {
        @Override
        public void handleError(Context ctx, Throwable e) {
            app.log.error("Request failed: " + ctx.method + " " + ctx.path);
            
            int code = -1;
            String msg = e.getMessage();
            
            // 根据异常类型设置不同的状态码和错误码
            if (e instanceof IllegalArgumentException) {
                ctx.status(400);
                code = 400;
            } else if (e instanceof SecurityException) {
                ctx.status(403);
                code = 403;
            } else {
                ctx.status(500);
                code = 500;
            }
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", code);
            response.put("msg", msg != null ? msg : "Internal Server Error");
            
            // 开发模式下返回堆栈
            if (app.devMode) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                response.put("stack", sw.toString());
            }
            
            ctx.json(response);
        }
    }
    
    /**
     * 创建自定义 App
     */
    public static App create() {
        App app = new App();
        app.use(new MyHandlerPlugin());
        app.use(new MyExceptionPlugin());
        return app;
    }
    
    // ==================== 便捷响应方法 ====================
    
    /**
     * 成功响应
     */
    public static void ok(Context ctx, Object data) {
        ctx.json(Maps.of("code", 0, "msg", "success", "data", data));
    }
    
    /**
     * 成功响应（无数据）
     */
    public static void ok(Context ctx) {
        ctx.json(Maps.of("code", 0, "msg", "success"));
    }
    
    /**
     * 失败响应
     */
    public static void fail(Context ctx, int code, String msg) {
        ctx.json(Maps.of("code", code, "msg", msg));
    }
}
