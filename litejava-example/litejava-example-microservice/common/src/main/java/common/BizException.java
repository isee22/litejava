package common;

import litejava.exception.LiteJavaException;

/**
 * 业务异常 - 继承框架 LiteJavaException
 * 
 * 使用示例：
 * if (userId == null) BizException.error(Err.PARAM_REQUIRED, "userId 不能为空");
 */
public class BizException extends LiteJavaException {
    
    public BizException(int code, String message) {
        super(message, 200, code); // HTTP 200, 业务码
    }
    
    /** 抛出业务异常 */
    public static void error(int code, String message) {
        throw new BizException(code, message);
    }
    
    /** 参数必填 */
    public static void paramRequired(String field) {
        throw new BizException(Err.PARAM_REQUIRED, field + " 不能为空");
    }
    
    /** 参数无效 */
    public static void paramInvalid(String message) {
        throw new BizException(Err.PARAM_INVALID, message);
    }
    
    /** 资源不存在 */
    public static void notFound(String resource) {
        throw new BizException(Err.NOT_FOUND, resource + "不存在");
    }
}
