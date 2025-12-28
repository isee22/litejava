package litejava.plugin;

import litejava.Plugin;
import litejava.util.Maps;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 响应格式插件 - 统一 ctx.ok() 和 ctx.fail() 的响应格式
 * 
 * <p>默认使用标准三件套格式：
 * <pre>{@code
 * // 成功
 * {"code": 0, "data": xxx, "msg": "success"}
 * 
 * // 失败
 * {"code": -1, "data": null, "msg": "错误信息"}
 * }</pre>
 * 
 * <h2>使用方式</h2>
 * <pre>{@code
 * // 方式一：使用默认格式
 * App app = LiteJava.create();  // 内置 ResultPlugin
 * 
 * // 方式二：使用简洁模式
 * app.use(new SimpleResultPlugin());
 * 
 * // 方式三：自定义格式（继承覆盖）
 * app.use(new ResultPlugin() {
 *     @Override
 *     public Object wrap(Object data) {
 *         return Maps.of("success", true, "result", data);
 *     }
 *     @Override
 *     public Object wrapError(String msg) {
 *         return Maps.of("success", false, "error", msg);
 *     }
 * });
 * }</pre>
 * 
 * <h2>配置文件</h2>
 * <pre>{@code
 * result:
 *   successCode: 0
 *   successMsg: success
 *   defaultErrorCode: -1
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see SimpleResultPlugin
 */
public class ResultPlugin extends Plugin {
    
    /** 单例插件，同类型自动替换 */
    @Override
    public boolean singleton() {
        return true;
    }
    
    /** 成功状态码 */
    public int successCode = 0;
    
    /** 成功消息 */
    public String successMsg = "success";
    
    /** 默认失败状态码 */
    public int defaultErrorCode = -1;
    
    @Override
    public void config() {
        // 从配置文件加载
        successCode = app.conf.getInt("result", "successCode", successCode);
        successMsg = app.conf.getString("result", "successMsg", successMsg);
        defaultErrorCode = app.conf.getInt("result", "defaultErrorCode", defaultErrorCode);
    }
    
    /**
     * 包装成功响应
     * 
     * <p>默认格式：{"code": 0, "data": xxx, "msg": "success"}
     * 
     * @param data 响应数据
     * @return 包装后的响应对象
     */
    public Object wrap(Object data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", successCode);
        resp.put("data", data);
        resp.put("msg", successMsg);
        return resp;
    }
    
    /**
     * 包装失败响应
     * 
     * <p>默认格式：{"code": -1, "data": null, "msg": "错误信息"}
     * 
     * @param msg 错误消息
     * @return 包装后的响应对象
     */
    public Object wrapError(String msg) {
        return wrapError(defaultErrorCode, msg);
    }
    
    /**
     * 包装失败响应（带错误码）
     * 
     * @param code 业务错误码
     * @param msg 错误消息
     * @return 包装后的响应对象
     */
    public Object wrapError(int code, String msg) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", code);
        resp.put("data", null);
        resp.put("msg", msg);
        return resp;
    }
    
    // ==================== 链式配置 ====================
    
    /**
     * 设置成功状态码
     */
    public ResultPlugin successCode(int code) {
        this.successCode = code;
        return this;
    }
    
    /**
     * 设置成功消息
     */
    public ResultPlugin successMsg(String msg) {
        this.successMsg = msg;
        return this;
    }
    
    /**
     * 设置默认失败状态码
     */
    public ResultPlugin defaultErrorCode(int code) {
        this.defaultErrorCode = code;
        return this;
    }
}
