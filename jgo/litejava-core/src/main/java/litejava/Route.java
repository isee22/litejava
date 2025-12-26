package litejava;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由定义 - 支持链式 API 添加文档元数据
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.post("/users", handler)
 *    .summary("创建用户")
 *    .param("name", String.class, true, "用户名")
 *    .param("age", Integer.class, false, "年龄")
 *    .response(200, User.class, "创建成功")
 *    .response(400, Error.class, "参数错误");
 * }</pre>
 */
public class Route {
    
    public final String method;
    public final String path;
    public final Handler handler;
    
    // 文档元数据
    public String summary;
    public String description;
    public String[] tags;
    public List<Param> params = new ArrayList<>();
    public List<Response> responses = new ArrayList<>();
    public Class<?> requestBody;
    public String requestBodyDesc;
    
    public Route(String method, String path, Handler handler) {
        this.method = method;
        this.path = path;
        this.handler = handler;
    }
    
    /**
     * 设置接口摘要
     */
    public Route summary(String summary) {
        this.summary = summary;
        return this;
    }
    
    /**
     * 设置接口描述
     */
    public Route desc(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * 设置标签（用于分组）
     */
    public Route tags(String... tags) {
        this.tags = tags;
        return this;
    }
    
    /**
     * 添加请求参数（Query/Path）
     */
    public Route param(String name, Class<?> type, boolean required, String desc) {
        params.add(new Param(name, type, required, desc));
        return this;
    }
    
    /**
     * 添加请求参数（必填）
     */
    public Route param(String name, Class<?> type, String desc) {
        return param(name, type, true, desc);
    }
    
    /**
     * 设置请求体类型
     */
    public Route body(Class<?> type, String desc) {
        this.requestBody = type;
        this.requestBodyDesc = desc;
        return this;
    }
    
    /**
     * 设置请求体类型
     */
    public Route body(Class<?> type) {
        return body(type, null);
    }
    
    /**
     * 添加响应定义
     */
    public Route response(int code, Class<?> type, String desc) {
        responses.add(new Response(code, type, desc));
        return this;
    }
    
    /**
     * 添加响应定义
     */
    public Route response(int code, Class<?> type) {
        return response(code, type, null);
    }
    
    @Override
    public String toString() {
        return method + " " + path;
    }
    
    /**
     * 请求参数定义
     */
    public static class Param {
        public final String name;
        public final Class<?> type;
        public final boolean required;
        public final String desc;
        
        public Param(String name, Class<?> type, boolean required, String desc) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.desc = desc;
        }
    }
    
    /**
     * 响应定义
     */
    public static class Response {
        public final int code;
        public final Class<?> type;
        public final String desc;
        
        public Response(int code, Class<?> type, String desc) {
            this.code = code;
            this.type = type;
            this.desc = desc;
        }
    }
}
