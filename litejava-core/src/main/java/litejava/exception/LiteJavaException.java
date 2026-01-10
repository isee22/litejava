package litejava.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all LiteJava framework exceptions.
 * 
 * - statusCode: HTTP 状态码 (200, 400, 500 等)
 * - code: 业务错误码 (40001, 41001 等)，默认等于 statusCode
 */
public class LiteJavaException extends RuntimeException {
    
    public int statusCode = 500;
    public int code = 500;
    public Map<String, Object> details = new HashMap<>();
    
    public LiteJavaException(String message) {
        super(message);
    }
    
    public LiteJavaException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public LiteJavaException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.code = statusCode;
    }
    
    public LiteJavaException(String message, int statusCode, int code) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }
    
    public LiteJavaException(String message, int statusCode, Map<String, Object> details) {
        super(message);
        this.statusCode = statusCode;
        this.code = statusCode;
        if (details != null) {
            this.details = details;
        }
    }
    
    /**
     * 创建业务异常 (HTTP 200, 自定义业务码)
     */
    public static LiteJavaException biz(int code, String message) {
        return new LiteJavaException(message, 200, code);
    }
}
