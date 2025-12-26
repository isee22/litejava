package litejava;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP 请求/响应上下文 - 统一的请求处理对象
 * 
 * <p>Context 是 LiteJava 处理 HTTP 请求的核心对象，借鉴 Gin 的设计：
 * <ul>
 *   <li>封装请求信息（method、path、headers、body）</li>
 *   <li>提供响应方法（json、html、redirect）</li>
 *   <li>支持参数绑定（路径参数、查询参数、JSON body）</li>
 *   <li>支持中间件间数据传递（state）</li>
 *   <li>支持请求中断（abort）</li>
 * </ul>
 * 
 * <h2>获取请求数据 (Gin-style)</h2>
 * <pre>{@code
 * // 路径参数 /users/:id
 * long id = ctx.pathParam("id", Long.class);
 * 
 * // 查询参数 ?page=1&size=10
 * int page = ctx.queryParam("page", Integer.class);
 * 
 * // JSON Body
 * Map<String, Object> data = ctx.bindJSON();
 * User user = ctx.bindJSON(User.class);
 * 
 * // 原始数据
 * byte[] raw = ctx.getRawData();
 * String str = ctx.getString();
 * 
 * // 表单/文件
 * Map<String, String> form = ctx.getForm();
 * Map<String, UploadedFile> files = ctx.getFiles();
 * }</pre>
 * 
 * <h2>发送响应 (Gin-style)</h2>
 * <pre>{@code
 * ctx.text("Hello World");           // 文本 (text/plain)
 * ctx.json(obj);                     // JSON
 * ctx.html("<h1>Hello</h1>");        // HTML
 * ctx.data(bytes, "image/png");      // 二进制
 * ctx.file(bytes, "report.pdf");     // 文件下载
 * ctx.redirect("/login");            // 重定向
 * ctx.render("user.html", data);     // 模板渲染
 * 
 * // 统一响应格式
 * ctx.ok(data);           // {"code": 0, "data": data, "msg": "success"}
 * ctx.fail("error msg");  // {"code": -1, "data": null, "msg": "error msg"}
 * }</pre>
 * 
 * <h2>中间件数据传递</h2>
 * <pre>{@code
 * // 在认证中间件中
 * ctx.state.put("user", currentUser);
 * 
 * // 在 handler 中获取
 * User user = (User) ctx.state.get("user");
 * }</pre>
 * 
 * <h2>请求中断（Gin-style Abort）</h2>
 * <pre>{@code
 * // 中断并返回 401
 * ctx.abortWithJson(401, Map.of("error", "Unauthorized"));
 * 
 * // 检查是否已中断
 * if (ctx.isAborted()) return;
 * }</pre>
 * 
 * @author LiteJava Team
 * @since 1.0.0
 * @see App 应用容器
 * @see Handler 请求处理器
 */
public class Context {
    
    // ==================== 预定义常量 (性能优化) ====================
    
    /** 预定义 Content-Type 常量，避免每次请求拼接字符串 */
    public static final String CT_TEXT = "text/plain; charset=utf-8";
    public static final String CT_JSON = "application/json; charset=utf-8";
    public static final String CT_HTML = "text/html; charset=utf-8";
    public static final String CT_BINARY = "application/octet-stream";
    
    // ==================== 全局配置 ====================
    
    /** 默认字符集，可通过配置文件 server.charset 修改 */
    public static Charset charset = StandardCharsets.UTF_8;
    
    /**
     * 设置全局字符集
     * @param charsetName 字符集名称，如 "UTF-8", "GBK", "ISO-8859-1"
     */
    public static void setCharset(String charsetName) {
        if (charsetName != null && !charsetName.isEmpty()) {
            charset = Charset.forName(charsetName);
        }
    }
    
    // ==================== 请求信息 ====================
    
    /** 应用实例引用 */
    public App app;
    
    /** HTTP 方法：GET, POST, PUT, DELETE, PATCH 等 */
    public String method;
    
    /** 请求路径，如 /users/123 */
    public String path;
    
    /** 查询字符串，如 page=1&size=10（不含 ?） */
    public String query;
    
    /** 客户端 IP 地址 */
    public String remoteAddr;
    
    /** 请求头 Map */
    public Map<String, String> headers = new HashMap<>(8);
    
    /** 路径参数 Map，如 /users/:id 中的 id */
    public Map<String, String> params = new HashMap<>(4);
    
    /** 查询参数 Map，解析自 query string */
    public Map<String, String> queryParams = new HashMap<>(8);
    
    /** 通配符路径，如 /files/*path 匹配的剩余路径 */
    public String wildcardPath;
    
    // ==================== 响应状态 ====================
    
    /** HTTP 响应状态码，默认 200 */
    private int responseStatus = 200;
    
    /** 响应头 Map */
    private Map<String, String> responseHeaders = new HashMap<>();
    
    /** 响应体字节数组 */
    private byte[] responseBody;
    
    /** 响应 Content-Type */
    private String responseContentType;
    
    // ==================== 请求体 ====================
    
    /** 原始请求体字节数组 */
    private byte[] requestBody;
    
    // ==================== 请求级状态 ====================
    
    /** 请求级状态存储，用于中间件间传递数据 */
    public Map<String, Object> state = new HashMap<>();
    
    /** 中断标志（Gin-style），true 表示中间件链已中断 */
    private boolean aborted = false;
    
    public Context() {}
    
    // ==================== 请求体设置（服务器插件调用）====================
    
    /**
     * 设置请求体（由服务器插件调用）
     * @param body 原始请求体字节数组
     */
    public void setRequestBody(byte[] body) {
        this.requestBody = body;
    }
    
    // ==================== 请求体读取 (Gin-style) ====================
    
    /**
     * 获取原始请求体字节数组 (Gin: GetRawData)
     * @return 请求体字节数组，无内容时返回空数组
     */
    public byte[] getRawData() {
        return requestBody != null ? requestBody : new byte[0];
    }
    
    /**
     * 获取请求体字符串
     * @return 请求体字符串
     */
    public String getString() {
        return new String(getRawData(), charset);
    }

    /**
     * 解析 JSON 请求体为 Map (Gin: ShouldBindJSON)
     * @return 解析后的 Map，空 body 返回空 Map
     */
    public Map<String, Object> bindJSON() {
        String body = getString();
        if (body == null || body.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        return app.json.parseMap(body);
    }
    
    /**
     * 解析 JSON 请求体为指定类型 (Gin: ShouldBindJSON)
     * @param clazz 目标类型
     * @return 解析后的对象
     */
    public <T> T bindJSON(Class<T> clazz) {
        return app.json.parse(getString(), clazz);
    }
    
    /**
     * 自动绑定请求参数到对象（Gin-style ShouldBind）
     * 
     * <p>根据 Content-Type 自动选择绑定方式：
     * <ul>
     *   <li>application/json → JSON 解析</li>
     *   <li>application/x-www-form-urlencoded → 表单解析</li>
     *   <li>其他 → 尝试 JSON 解析</li>
     * </ul>
     * 
     * <pre>{@code
     * User user = ctx.bind(User.class);
     * }</pre>
     * 
     * @param clazz 目标类型
     * @return 绑定后的对象
     */
    public <T> T bind(Class<T> clazz) {
        String contentType = headers.getOrDefault("Content-Type", 
                            headers.getOrDefault("content-type", ""));
        
        if (contentType.contains("application/json")) {
            return bindJSON(clazz);
        } else if (contentType.contains("application/x-www-form-urlencoded")) {
            // Form 转 JSON 再解析
            Map<String, String> form = getForm();
            String json = app.json.stringify(form);
            return app.json.parse(json, clazz);
        } else {
            // 默认尝试 JSON
            String body = getString();
            if (body != null && body.trim().startsWith("{")) {
                return bindJSON(clazz);
            }
            return app.json.parse("{}", clazz);
        }
    }
    
    /**
     * 绑定到 Map
     */
    public Map<String, Object> bind() {
        String contentType = headers.getOrDefault("Content-Type", 
                            headers.getOrDefault("content-type", ""));
        
        if (contentType.contains("application/json")) {
            return bindJSON();
        } else if (contentType.contains("application/x-www-form-urlencoded")) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.putAll(getForm());
            return result;
        } else {
            String body = getString();
            if (body != null && body.trim().startsWith("{")) {
                return bindJSON();
            }
            return new LinkedHashMap<>();
        }
    }
    
    /**
     * 解析 URL 编码的表单数据
     * @return 表单字段 Map
     */
    public Map<String, String> getForm() {
        Map<String, String> result = new LinkedHashMap<>();
        String body = getString();
        if (body == null || body.isEmpty()) return result;
        
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), charset);
                String value = idx < pair.length() - 1 ? 
                    URLDecoder.decode(pair.substring(idx + 1), charset) : "";
                result.put(key, value);
            }
        }
        return result;
    }
    
    /**
     * 解析 multipart/form-data 文件上传
     * @return 文件 Map，key 为表单字段名
     */
    public Map<String, UploadedFile> getFiles() {
        Map<String, UploadedFile> result = new LinkedHashMap<>();
        String contentType = headers.get("Content-Type");
        if (contentType == null) contentType = headers.get("content-type");
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            return result;
        }
        
        int boundaryIdx = contentType.indexOf("boundary=");
        if (boundaryIdx < 0) return result;
        
        String boundary = contentType.substring(boundaryIdx + 9);
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        
        String bodyStr = getString();
        for (String part : bodyStr.split("--" + boundary)) {
            if (part.trim().isEmpty() || part.equals("--")) continue;
            parseMultipartPart(part, result);
        }
        
        return result;
    }
    
    private void parseMultipartPart(String part, Map<String, UploadedFile> result) {
        int headerEnd = part.indexOf("\r\n\r\n");
        if (headerEnd < 0) headerEnd = part.indexOf("\n\n");
        if (headerEnd < 0) return;
        
        String headerSection = part.substring(0, headerEnd);
        String content = part.substring(headerEnd + (part.contains("\r\n\r\n") ? 4 : 2));
        
        if (content.endsWith("--\r\n") || content.endsWith("--\n")) {
            content = content.substring(0, content.lastIndexOf("--"));
        }
        if (content.endsWith("\r\n")) {
            content = content.substring(0, content.length() - 2);
        } else if (content.endsWith("\n")) {
            content = content.substring(0, content.length() - 1);
        }
        
        String name = null, filename = null;
        String fileContentType = "application/octet-stream";
        
        for (String line : headerSection.split("\r?\n")) {
            if (line.toLowerCase().startsWith("content-disposition:")) {
                int nameIdx = line.indexOf("name=\"");
                if (nameIdx >= 0) {
                    int nameEnd = line.indexOf("\"", nameIdx + 6);
                    if (nameEnd > nameIdx) name = line.substring(nameIdx + 6, nameEnd);
                }
                int filenameIdx = line.indexOf("filename=\"");
                if (filenameIdx >= 0) {
                    int filenameEnd = line.indexOf("\"", filenameIdx + 10);
                    if (filenameEnd > filenameIdx) filename = line.substring(filenameIdx + 10, filenameEnd);
                }
            } else if (line.toLowerCase().startsWith("content-type:")) {
                fileContentType = line.substring(13).trim();
            }
        }
        
        if (name != null && filename != null) {
            UploadedFile file = new UploadedFile();
            file.name = filename;
            file.contentType = fileContentType;
            file.content = content.getBytes(charset);
            file.size = file.content.length;
            result.put(name, file);
        }
    }

    // ==================== 响应方法 (Gin-style) ====================
    
    /**
     * 设置 HTTP 状态码
     * @param code HTTP 状态码
     * @return this（支持链式调用）
     */
    public Context status(int code) {
        this.responseStatus = code;
        return this;
    }
    
    /**
     * 设置响应头
     * @param name 头名称
     * @param value 头值
     * @return this
     */
    public Context header(String name, String value) {
        this.responseHeaders.put(name, value);
        return this;
    }
    
    /**
     * 发送文本响应 (Gin: c.String)
     * @param content 文本内容
     * @return this
     */
    public Context text(String content) {
        this.responseBody = content.getBytes(charset);
        if (responseContentType == null && !responseHeaders.containsKey("Content-Type")) {
            this.responseHeaders.put("Content-Type", CT_TEXT);
        }
        return this;
    }
    
    /**
     * 发送二进制响应 (Gin: c.Data)
     * @param content 字节数组
     * @param contentType Content-Type
     * @return this
     */
    public Context data(byte[] content, String contentType) {
        this.responseBody = content;
        this.responseHeaders.put("Content-Type", contentType);
        return this;
    }
    
    /**
     * 发送二进制响应（默认 application/octet-stream）
     * @param content 字节数组
     * @return this
     */
    public Context data(byte[] content) {
        return data(content, CT_BINARY);
    }
    
    /**
     * 发送 JSON 响应
     * 
     * <pre>{@code
     * ctx.json(Map.of("id", 1, "name", "test"));
     * ctx.json(user);  // 自动序列化对象
     * }</pre>
     * 
     * @param obj 要序列化的对象
     * @return this
     */
    public Context json(Object obj) {
        this.responseBody = app.json.stringifyBytes(obj);
        this.responseHeaders.put("Content-Type", CT_JSON);
        this.responseContentType = null;
        return this;
    }
    
    /**
     * 发送 HTML 响应
     * @param content HTML 内容
     * @return this
     */
    public Context html(String content) {
        this.responseBody = content.getBytes(charset);
        this.responseHeaders.put("Content-Type", CT_HTML);
        this.responseContentType = null;
        return this;
    }
    
    /**
     * 渲染模板
     * @param template 模板名称
     * @param data 模板数据
     * @return this
     */
    public Context render(String template, Map<String, Object> data) {
        String html = app.view.render(template, data);
        return html(html);
    }
    
    /**
     * 重定向
     * @param url 目标 URL
     * @return this
     */
    public Context redirect(String url) {
        this.responseStatus = 302;
        this.responseHeaders.put("Location", url);
        return this;
    }
    
    /**
     * 文件下载
     */
    public Context file(byte[] content, String filename) {
        this.responseBody = content;
        this.responseContentType = "application/octet-stream";
        this.responseHeaders.put("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        return this;
    }
    
    public Context file(java.io.File file) throws java.io.IOException {
        return file(java.nio.file.Files.readAllBytes(file.toPath()), file.getName());
    }
    
    // ==================== 统一响应格式 ====================
    
    /**
     * 成功响应（统一格式）
     * 
     * <pre>{@code
     * ctx.ok(data);
     * // 输出: {"code": 0, "data": data, "msg": "success"}
     * }</pre>
     * 
     * @param data 响应数据
     * @return this
     */
    public Context ok(Object data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", 0);
        resp.put("data", data);
        resp.put("msg", "success");
        return json(resp);
    }
    
    /**
     * 成功响应（无数据）
     * @return this
     */
    public Context ok() {
        return ok(null);
    }
    
    /**
     * 失败响应
     * 
     * <pre>{@code
     * ctx.fail("参数错误");
     * // 输出: {"code": -1, "data": null, "msg": "参数错误"}
     * }</pre>
     * 
     * @param msg 错误消息
     * @return this
     */
    public Context fail(String msg) {
        return fail(-1, msg);
    }
    
    /**
     * 失败响应（自定义错误码）
     * @param code 业务错误码
     * @param msg 错误消息
     * @return this
     */
    public Context fail(int code, String msg) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", code);
        resp.put("data", null);
        resp.put("msg", msg);
        return json(resp);
    }
    
    /**
     * 失败响应（带 HTTP 状态码）
     * @param httpStatus HTTP 状态码
     * @param code 业务错误码
     * @param msg 错误消息
     * @return this
     */
    public Context fail(int httpStatus, int code, String msg) {
        status(httpStatus);
        return fail(code, msg);
    }
    
    // ==================== 参数获取 ====================
    
    /**
     * 获取路径参数 (Gin: c.Param, Javalin: ctx.pathParam)
     * 
     * <p>路径参数是 URL 路径中的动态部分，通过 :name 定义。
     * 
     * <h3>示例</h3>
     * <pre>{@code
     * // 路由定义
     * app.get("/users/:id", ctx -> {
     *     String id = ctx.pathParam("id");  // 获取路径参数
     * });
     * 
     * app.get("/files/*path", ctx -> {
     *     String path = ctx.pathParam("path");  // 通配符参数
     * });
     * 
     * // 请求: GET /users/123
     * // ctx.pathParam("id") => "123"
     * 
     * // 请求: GET /files/images/logo.png
     * // ctx.pathParam("path") => "images/logo.png"
     * }</pre>
     * 
     * @param name 参数名（路由中 :name 的 name 部分）
     * @return 参数值，不存在返回 null
     */
    public String pathParam(String name) {
        return params.get(name);
    }
    
    /**
     * 获取路径参数并转换类型
     * 
     * <pre>{@code
     * // 路由: /users/:id
     * // 请求: GET /users/123
     * 
     * Long id = ctx.pathParam("id", Long.class);  // => 123L
     * }</pre>
     * 
     * @param name 参数名
     * @param clazz 目标类型（支持 String, Integer, Long, Double, Boolean）
     * @return 转换后的值，参数不存在返回 null
     */
    public <T> T pathParam(String name, Class<T> clazz) {
        String value = params.get(name);
        if (value == null) return null;
        return convert(value, clazz);
    }
    
    /**
     * 获取查询参数 (Gin: c.Query, Javalin: ctx.queryParam)
     * 
     * <p>查询参数是 URL 中 ? 后面的键值对，GET/POST/PUT/DELETE 等所有请求都可以带。
     * 
     * <h3>GET 请求示例</h3>
     * <pre>{@code
     * // GET /users?q=john&page=1&size=10
     * app.get("/users", ctx -> {
     *     String keyword = ctx.queryParam("q");      // => "john"
     *     String page = ctx.queryParam("page");      // => "1"
     * });
     * }</pre>
     * 
     * <h3>POST 请求示例</h3>
     * <pre>{@code
     * // POST /users?notify=true
     * // Body: {"name": "John", "age": 25}
     * app.post("/users", ctx -> {
     *     boolean notify = ctx.queryParam("notify", Boolean.class);  // URL 参数
     *     User user = ctx.bindJSON(User.class);                      // Body 参数
     * });
     * }</pre>
     * 
     * @param name 参数名
     * @return 参数值，不存在返回 null
     */
    public String queryParam(String name) {
        return queryParams.get(name);
    }
    
    /**
     * 获取查询参数（带默认值）
     * 
     * <pre>{@code
     * // 请求: GET /users?page=2
     * String page = ctx.queryParam("page", "1");  // => "2"
     * String size = ctx.queryParam("size", "10"); // => "10" (使用默认值)
     * }</pre>
     * 
     * @param name 参数名
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public String queryParam(String name, String defaultValue) {
        String value = queryParams.get(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取查询参数并转换类型
     * 
     * <pre>{@code
     * // 请求: GET /users?page=2&active=true
     * Integer page = ctx.queryParam("page", Integer.class);    // => 2
     * Boolean active = ctx.queryParam("active", Boolean.class); // => true
     * }</pre>
     * 
     * @param name 参数名
     * @param clazz 目标类型（支持 String, Integer, Long, Double, Boolean）
     * @return 转换后的值，参数不存在返回 null
     */
    public <T> T queryParam(String name, Class<T> clazz) {
        String value = queryParams.get(name);
        if (value == null) return null;
        return convert(value, clazz);
    }
    
    /**
     * 获取表单参数 (Gin: c.PostForm)
     * 
     * <p>表单参数来自 POST 请求的 application/x-www-form-urlencoded body。
     * 
     * <h3>示例</h3>
     * <pre>{@code
     * // POST /login
     * // Content-Type: application/x-www-form-urlencoded
     * // Body: username=admin&password=123456
     * 
     * app.post("/login", ctx -> {
     *     String username = ctx.formParam("username");  // => "admin"
     *     String password = ctx.formParam("password");  // => "123456"
     * });
     * }</pre>
     * 
     * @param name 参数名
     * @return 参数值，不存在返回 null
     */
    public String formParam(String name) {
        return getForm().get(name);
    }
    
    /**
     * 获取表单参数（带默认值）
     * @param name 参数名
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public String formParam(String name, String defaultValue) {
        String value = getForm().get(name);
        return value != null ? value : defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convert(String value, Class<T> clazz) {
        if (clazz == String.class) return (T) value;
        if (clazz == Integer.class || clazz == int.class) return (T) Integer.valueOf(value);
        if (clazz == Long.class || clazz == long.class) return (T) Long.valueOf(value);
        if (clazz == Double.class || clazz == double.class) return (T) Double.valueOf(value);
        if (clazz == Boolean.class || clazz == boolean.class) return (T) Boolean.valueOf(value);
        throw new IllegalArgumentException("Unsupported type: " + clazz);
    }
    
    // Plugin access
    
    @SuppressWarnings("unchecked")
    public <T> T plugin(Class<T> clazz) {
        if (app != null && app.plugins != null) {
            return (T) app.plugins.get(clazz.getSimpleName());
        }
        return null;
    }
    
    // Response getters
    
    public int getResponseStatus() {
        return responseStatus;
    }
    
    public Map<String, String> getResponseHeaders() {
        // 直接在 responseHeaders 中设置 Content-Type，避免创建新 Map
        if (responseContentType != null && !responseHeaders.containsKey("Content-Type")) {
            responseHeaders.put("Content-Type", responseContentType);
        }
        return responseHeaders;
    }
    
    public byte[] getResponseBody() {
        return responseBody != null ? responseBody : EMPTY_BODY;
    }
    
    private static final byte[] EMPTY_BODY = new byte[0];
    
    // ==================== Abort 机制（Gin-style）====================
    
    /**
     * 中断中间件链
     * 
     * <p>调用后，后续中间件和 handler 将不再执行。
     * 通常在认证失败等场景使用。
     */
    public void abort() {
        this.aborted = true;
    }
    
    /**
     * 中断并设置 HTTP 状态码
     * @param code HTTP 状态码
     */
    public void abortWithStatus(int code) {
        this.aborted = true;
        status(code);
    }
    
    /**
     * 中断并返回 JSON 错误响应
     * 
     * <pre>{@code
     * ctx.abortWithJson(401, Map.of("error", "Unauthorized"));
     * }</pre>
     * 
     * @param code HTTP 状态码
     * @param obj JSON 响应对象
     */
    public void abortWithJson(int code, Object obj) {
        this.aborted = true;
        status(code);
        json(obj);
    }
    
    /**
     * 检查是否已中断
     * @return true 表示已中断
     */
    public boolean isAborted() {
        return aborted;
    }
    
    // ==================== Cookie 支持 ====================
    
    /**
     * 获取 Cookie 值
     * @param name Cookie 名称
     * @return Cookie 值，不存在返回 null
     */
    public String cookie(String name) {
        String cookieHeader = header("Cookie");
        if (cookieHeader == null) cookieHeader = header("cookie");
        if (cookieHeader == null) return null;
        
        for (String part : cookieHeader.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return URLDecoder.decode(kv[1], charset);
            }
        }
        return null;
    }
    
    /**
     * 设置 Cookie（简化版）
     * @param name Cookie 名称
     * @param value Cookie 值
     * @param maxAge 过期时间（秒），0 表示会话 Cookie
     * @return this
     */
    public Context setCookie(String name, String value, int maxAge) {
        return setCookie(name, value, maxAge, "/", "", false, false);
    }
    
    /**
     * 设置 Cookie（完整参数）
     * @param name Cookie 名称
     * @param value Cookie 值
     * @param maxAge 过期时间（秒）
     * @param path 路径
     * @param domain 域名
     * @param secure 是否仅 HTTPS
     * @param httpOnly 是否禁止 JS 访问
     * @return this
     */
    public Context setCookie(String name, String value, int maxAge, String path, 
                             String domain, boolean secure, boolean httpOnly) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(URLEncoder.encode(value, charset));
        
        if (maxAge >= 0) {
            sb.append("; Max-Age=").append(maxAge);
        }
        if (path != null && !path.isEmpty()) {
            sb.append("; Path=").append(path);
        }
        if (domain != null && !domain.isEmpty()) {
            sb.append("; Domain=").append(domain);
        }
        if (secure) {
            sb.append("; Secure");
        }
        if (httpOnly) {
            sb.append("; HttpOnly");
        }
        
        header("Set-Cookie", sb.toString());
        return this;
    }
    
    /**
     * 删除 Cookie
     * @param name Cookie 名称
     * @return this
     */
    public Context deleteCookie(String name) {
        return setCookie(name, "", -1, "/", "", false, false);
    }
    
    // ==================== 客户端 IP ====================
    
    /**
     * 获取客户端真实 IP（支持代理）
     * 
     * <p>按优先级检查以下请求头：
     * <ol>
     *   <li>X-Forwarded-For</li>
     *   <li>X-Real-IP</li>
     *   <li>X-Client-IP</li>
     *   <li>CF-Connecting-IP（Cloudflare）</li>
     * </ol>
     * 
     * @return 客户端 IP 地址
     */
    public String clientIP() {
        // 按优先级检查代理头
        String[] proxyHeaders = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "X-Client-IP",
            "CF-Connecting-IP"  // Cloudflare
        };
        
        for (String h : proxyHeaders) {
            String ip = header(h);
            if (ip != null && !ip.isEmpty()) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                int idx = ip.indexOf(',');
                return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
            }
        }
        
        return remoteAddr;
    }
    
    // ==================== Header 便捷方法 ====================
    
    /**
     * 获取请求头（不区分大小写）
     * @param name 头名称
     * @return 头值，不存在返回 null
     */
    public String header(String name) {
        String value = headers.get(name);
        if (value != null) return value;
        
        // 尝试不区分大小写
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * 获取 Content-Type
     */
    public String contentType() {
        return header("Content-Type");
    }
    
    /**
     * 获取 User-Agent
     */
    public String userAgent() {
        return header("User-Agent");
    }
    
    // ==================== Query/URI 绑定（Gin-style）====================
    
    /**
     * 绑定查询参数到对象（Gin-style ShouldBindQuery）
     * @param clazz 目标类型
     * @return 绑定后的对象
     */
    public <T> T bindQuery(Class<T> clazz) {
        String json = app.json.stringify(queryParams);
        return app.json.parse(json, clazz);
    }
    
    /**
     * 绑定路径参数到对象（Gin-style ShouldBindUri）
     * @param clazz 目标类型
     * @return 绑定后的对象
     */
    public <T> T bindUri(Class<T> clazz) {
        String json = app.json.stringify(params);
        return app.json.parse(json, clazz);
    }
    
    // ==================== 对象池支持 ====================
    
    /**
     * 重置 Context 状态（用于对象池复用）
     * 
     * <p>服务器插件使用对象池时，在请求处理完成后调用此方法
     * 清理状态，以便 Context 对象可以被下一个请求复用。
     */
    public void reset() {
        app = null;
        method = null;
        path = null;
        query = null;
        remoteAddr = null;
        wildcardPath = null;
        headers.clear();
        params.clear();
        queryParams.clear();
        state.clear();
        responseStatus = 200;
        responseHeaders.clear();
        responseBody = null;
        responseContentType = null;
        requestBody = null;
        aborted = false;
    }
}
