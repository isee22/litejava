package litejava.plugin;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import litejava.Plugin;

/**
 * JSON 插件基类 - 定义 JSON 序列化/反序列化接口
 * 
 * <p>这是一个抽象基类，具体实现由子类提供：
 * <ul>
 *   <li>GoJsonPlugin - 零依赖内置实现（litejava-core）</li>
 *   <li>JacksonPlugin - Jackson 实现（litejava-plugins）</li>
 *   <li>GsonPlugin - Gson 实现（litejava-plugins）</li>
 *   <li>FastjsonPlugin - Fastjson 实现（litejava-plugins）</li>
 * </ul>
 * 
 * <h2>使用方式</h2>
 * <pre>{@code
 * // 在 handler 中使用
 * app.get("/users/:id", ctx -> {
 *     User user = userService.get(ctx.paramLong("id"));
 *     ctx.json(user);  // 自动使用注册的 JsonPlugin 序列化
 * });
 * 
 * app.post("/users", ctx -> {
 *     User user = ctx.bindJSON(User.class);  // 自动反序列化
 *     userService.create(user);
 *     ctx.ok(user);
 * });
 * }</pre>
 * 
 * <h2>直接使用 JsonPlugin</h2>
 * <pre>{@code
 * // 序列化
 * String json = app.json.stringify(user);
 * byte[] bytes = app.json.stringifyBytes(user);
 * 
 * // 反序列化
 * User user = app.json.parse(json, User.class);
 * Map<String, Object> map = app.json.parseMap(json);
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see Context#json(Object) JSON 响应
 * @see Context#bodyJson() 解析 JSON 请求体
 */
public class JsonPlugin extends Plugin {
    
    /**
     * 将对象序列化为 JSON 字符串
     * @param obj 要序列化的对象
     * @return JSON 字符串
     */
    public String stringify(Object obj) {
        throw new UnsupportedOperationException("No JsonPlugin implementation");
    }
    
    /**
     * 将对象序列化为 JSON 字节数组
     * <p>子类可覆盖此方法提供更高效的实现，避免中间 String 转换
     * 
     * @param obj 要序列化的对象
     * @return JSON 字节数组
     */
    public byte[] stringifyBytes(Object obj) {
        return stringify(obj).getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * 将 JSON 字符串反序列化为指定类型
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @return 反序列化后的对象
     */
    public <T> T parse(String json, Class<T> clazz) {
        throw new UnsupportedOperationException("No JsonPlugin implementation");
    }
    
    /**
     * 将 JSON 字符串反序列化为 Map
     * @param json JSON 字符串
     * @return Map 对象
     */
    public Map<String, Object> parseMap(String json) {
        throw new UnsupportedOperationException("No JsonPlugin implementation");
    }
    
    /**
     * 将对象转换为指定类型（不走 JSON 字符串）
     * <p>用于将 Map/List 等动态类型转换为 VO 对象
     * 
     * @param obj 源对象（通常是 Map 或 List）
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public <T> T convert(Object obj, Class<T> clazz) {
        // 默认实现：走 stringify + parse（子类可覆盖提供更高效实现）
        String json = stringify(obj);
        return parse(json, clazz);
    }
}
