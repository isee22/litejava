package litejava.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all LiteJava framework exceptions.
 * 
 * <h2>为什么有两个 code？</h2>
 * <p>LiteJava 的异常设计包含两个维度的错误码：
 * <ul>
 *   <li><strong>statusCode</strong>: HTTP 状态码 (200, 400, 404, 500) - 给浏览器/网关/负载均衡器看</li>
 *   <li><strong>code</strong>: 业务错误码 (40001, 41001) - 给前端业务逻辑看</li>
 * </ul>
 * 
 * <p><strong>为什么不能只用一个 code？</strong>
 * <table border="1">
 *   <tr>
 *     <th>场景</th>
 *     <th>statusCode</th>
 *     <th>code</th>
 *     <th>说明</th>
 *   </tr>
 *   <tr>
 *     <td>业务失败</td>
 *     <td>200</td>
 *     <td>41001</td>
 *     <td>HTTP 成功（网关放行），但业务失败（前端处理）</td>
 *   </tr>
 *   <tr>
 *     <td>HTTP 错误</td>
 *     <td>404</td>
 *     <td>404</td>
 *     <td>HTTP 失败（网关拦截），code = statusCode</td>
 *   </tr>
 * </table>
 * 
 * <h2>使用场景</h2>
 * <pre>{@code
 * // 场景1: 业务异常 (HTTP 200 + 业务错误码)
 * throw LiteJavaException.biz(41001, "用户不存在");
 * // 响应: HTTP 200, {code: 41001, msg: "用户不存在"}
 * 
 * // 场景2: HTTP 异常 (HTTP 4xx/5xx)
 * throw LiteJavaException.badRequest("参数错误");
 * throw LiteJavaException.notFound("资源不存在");
 * // 响应: HTTP 404, {code: 404, msg: "资源不存在"}
 * 
 * // 场景3: 系统异常 (HTTP 500)
 * throw LiteJavaException.internal("数据库连接失败");
 * // 响应: HTTP 500, {code: 500, msg: "数据库连接失败"}
 * }</pre>
 * 
 * <h2>为什么需要两个 code？</h2>
 * <p>HTTP 状态码和业务错误码是两个维度：
 * <ul>
 *   <li>HTTP 状态码：告诉网关/负载均衡器请求是否成功（200=成功，4xx=客户端错误，5xx=服务端错误）</li>
 *   <li>业务错误码：告诉前端具体的业务错误类型（41001=用户不存在，42001=订单不存在）</li>
 * </ul>
 * 
 * <p>业务异常通常返回 HTTP 200（请求成功），但 code 不为 0（业务失败）。
 * 这样网关不会误判为服务故障，前端可以根据 code 做精确的错误处理。
 */
public class LiteJavaException extends RuntimeException {
    
    /** HTTP 状态码 (200, 400, 404, 500 等) */
    public int statusCode = 500;
    
    /** 业务错误码 (40001, 41001 等) */
    public int code = 0;
    
    /** 额外的错误详情 */
    public Map<String, Object> details = new HashMap<>();
    
    /**
     * 基础构造函数（保护级别，推荐使用静态工厂方法）
     * 
     * <p><strong>注意</strong>：构造函数参数容易混淆，推荐使用静态工厂方法：
     * <ul>
     *   <li>{@link #biz(int, String)} - 业务异常</li>
     *   <li>{@link #badRequest(String)} - HTTP 400</li>
     *   <li>{@link #notFound(String)} - HTTP 404</li>
     *   <li>{@link #internal(String)} - HTTP 500</li>
     * </ul>
     */
    protected LiteJavaException(String message) {
        super(message);
    }
    
    protected LiteJavaException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * HTTP 异常构造函数
     * @param message 错误消息
     * @param httpStatus HTTP 状态码（同时设置 code = httpStatus）
     */
    protected LiteJavaException(String message, int httpStatus) {
        super(message);
        this.statusCode = httpStatus;
        this.code = httpStatus;  // HTTP 异常：code = statusCode
    }
    
    /**
     * 业务异常构造函数
     * @param message 错误消息
     * @param httpStatus HTTP 状态码（通常为 200）
     * @param bizCode 业务错误码（如 41001）
     */
    protected LiteJavaException(String message, int httpStatus, int bizCode) {
        super(message);
        this.statusCode = httpStatus;
        this.code = bizCode;  // 业务异常：code = bizCode
    }
    
    protected LiteJavaException(String message, int httpStatus, Map<String, Object> details) {
        super(message);
        this.statusCode = httpStatus;
        this.code = httpStatus;
        if (details != null) {
            this.details = details;
        }
    }
    
    // ==================== 业务异常 (HTTP 200) ====================
    
    /**
     * 创建业务异常 (HTTP 200 + 自定义业务码)
     * 
     * <p>用于业务逻辑错误，HTTP 层认为请求成功，但业务层失败。
     * 
     * @param code 业务错误码 (建议 >= 10000，避免与 HTTP 状态码冲突)
     * @param message 错误消息
     * @return LiteJavaException
     */
    public static LiteJavaException biz(int code, String message) {
        return new LiteJavaException(message, 200, code);
    }
    
    // ==================== HTTP 异常 (4xx Client Error) ====================
    
    /**
     * 400 Bad Request - 参数错误
     */
    public static LiteJavaException badRequest(String message) {
        return new LiteJavaException(message, 400);
    }
    
    /**
     * 401 Unauthorized - 未认证
     */
    public static LiteJavaException unauthorized(String message) {
        return new LiteJavaException(message, 401);
    }
    
    /**
     * 403 Forbidden - 无权限
     */
    public static LiteJavaException forbidden(String message) {
        return new LiteJavaException(message, 403);
    }
    
    /**
     * 404 Not Found - 资源不存在
     */
    public static LiteJavaException notFound(String message) {
        return new LiteJavaException(message, 404);
    }
    
    /**
     * 429 Too Many Requests - 请求过于频繁
     */
    public static LiteJavaException tooManyRequests(String message) {
        return new LiteJavaException(message, 429);
    }
    
    // ==================== HTTP 异常 (5xx Server Error) ====================
    
    /**
     * 500 Internal Server Error - 服务器内部错误
     */
    public static LiteJavaException internal(String message) {
        return new LiteJavaException(message, 500);
    }
    
    /**
     * 500 Internal Server Error - 服务器内部错误（带原因）
     */
    public static LiteJavaException internal(String message, Throwable cause) {
        return new LiteJavaException(message, cause);
    }
    
    /**
     * 503 Service Unavailable - 服务不可用
     */
    public static LiteJavaException unavailable(String message) {
        return new LiteJavaException(message, 503);
    }
}
