package litejava.plugins.vt;

import litejava.plugin.JsonPlugin;

import java.util.Map;

/**
 * Go 风格 JSON 插件 - 基于 GoJson 工具类
 * 
 * <pre>{@code
 * app.use(new GoJsonPlugin());
 * 
 * // 在 Handler 中
 * ctx.json(Map.of("code", 0, "msg", "ok"));
 * User user = ctx.bindJSON(User.class);
 * }</pre>
 */
public class GoJsonPlugin extends JsonPlugin {
    
    @Override
    public String stringify(Object obj) {
        return GoJson.marshalString(obj);
    }
    
    @Override
    public byte[] stringifyBytes(Object obj) {
        return GoJson.marshal(obj);
    }
    
    @Override
    public <T> T parse(String json, Class<T> clazz) {
        return GoJson.unmarshal(json, clazz);
    }
    
    @Override
    public Map<String, Object> parseMap(String json) {
        return GoJson.unmarshal(json);
    }
}
