package litejava.plugin;

import litejava.util.Maps;

/**
 * 简洁响应格式插件
 * 
 * <p>返回格式：
 * <pre>{@code
 * // 成功 - 直接返回数据
 * {"id": 1, "name": "test"}
 * 
 * // 失败
 * {"error": "错误信息"}
 * 
 * // 失败（带错误码）
 * {"error": "错误信息", "code": 1001}
 * }</pre>
 * 
 * <p>适合 RESTful API 风格，成功时直接返回数据，失败时返回 error 字段。
 * 客户端通过 HTTP 状态码判断成功/失败。
 * 
 * <h2>使用方式</h2>
 * <pre>{@code
 * app.use(new SimpleResultPlugin());
 * 
 * // 然后
 * ctx.ok(user);  // 直接输出 {"id": 1, "name": "test"}
 * ctx.fail("参数错误");  // 输出 {"error": "参数错误"}
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see ResultPlugin
 */
public class SimpleResultPlugin extends ResultPlugin {
    
    @Override
    public Object wrap(Object data) {
        return data;
    }
    
    @Override
    public Object wrapError(String msg) {
        return Maps.of("error", msg);
    }
    
    @Override
    public Object wrapError(int code, String msg) {
        return Maps.of("error", msg, "code", code);
    }
}
