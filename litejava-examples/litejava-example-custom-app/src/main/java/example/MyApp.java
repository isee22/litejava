package example;

import litejava.App;
import litejava.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义 App 示例 - 展示如何继承 App 重写行为
 * 
 * <p>可重写的方法：
 * <ul>
 *   <li>{@link #handle(Context)} - 自定义请求处理流程</li>
 *   <li>{@link #handleError(Context, Exception)} - 自定义错误处理</li>
 * </ul>
 * 
 * <p>本示例演示：
 * <ul>
 *   <li>统一的 API 响应格式：{code, msg, data}</li>
 *   <li>请求耗时日志</li>
 *   <li>自定义错误响应</li>
 * </ul>
 */
public class MyApp extends App {
    
    /**
     * 重写请求处理 - 添加耗时日志
     */
    @Override
    public void handle(Context ctx) throws Exception {
        long start = System.currentTimeMillis();
        try {
            super.handle(ctx);
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info(ctx.method + " " + ctx.path + " " + ctx.getResponseStatus() + " " + cost + "ms");
        }
    }
    
    /**
     * 重写错误处理 - 统一错误响应格式
     */
    @Override
    public void handleError(Context ctx, Exception e) {
        log.error("Request failed: " + ctx.method + " " + ctx.path, e);
        
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
        if (devMode) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            response.put("stack", sw.toString());
        }
        
        ctx.json(response);
    }
    
    // ==================== 便捷响应方法 ====================
    
    /**
     * 成功响应
     */
    public static void ok(Context ctx, Object data) {
        ctx.json(Map.of("code", 0, "msg", "success", "data", data));
    }
    
    /**
     * 成功响应（无数据）
     */
    public static void ok(Context ctx) {
        ctx.json(Map.of("code", 0, "msg", "success"));
    }
    
    /**
     * 失败响应
     */
    public static void fail(Context ctx, int code, String msg) {
        ctx.json(Map.of("code", code, "msg", msg));
    }
}
