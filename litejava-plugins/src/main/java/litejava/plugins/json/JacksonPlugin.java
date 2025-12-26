package litejava.plugins.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import litejava.plugin.JsonPlugin;

import java.util.Map;

/**
 * Jackson JSON 插件 - 提供 JSON 序列化/反序列化
 * 
 * <h2>依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.fasterxml.jackson.core</groupId>
 *     <artifactId>jackson-databind</artifactId>
 *     <version>2.15.3</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new JacksonPlugin());
 * 
 * // 在 Handler 中
 * ctx.json(Map.of("name", "test"));           // 响应 JSON
 * Map<String, Object> body = ctx.bindJSON();  // 解析请求体
 * User user = ctx.bindJSON(User.class);       // 解析为对象
 * 
 * // 直接使用 ObjectMapper
 * JacksonPlugin.mapper.writeValueAsString(obj);
 * }</pre>
 */
public class JacksonPlugin extends JsonPlugin {
    
    public static final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String stringify(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON stringify failed", e);
        }
    }
    
    /**
     * 直接序列化为 byte[]，避免中间 String 转换
     */
    @Override
    public byte[] stringifyBytes(Object obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON stringify failed", e);
        }
    }
    
    @Override
    public <T> T parse(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON parse failed", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseMap(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON parse failed", e);
        }
    }
}
