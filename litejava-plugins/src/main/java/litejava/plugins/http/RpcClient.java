package litejava.plugins.http;

import litejava.App;
import litejava.Plugin;
import litejava.plugins.microservice.DiscoveryPlugin;
import litejava.plugins.microservice.DiscoveryPlugin.ServiceInstance;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RPC 客户端插件 - 基于 OkHttp 的高性能服务间调用
 * 
 * 特性：
 * 1. 连接池复用（默认 100 连接，5 分钟空闲超时）
 * 2. 服务发现集成（自动从 Consul/Nacos 获取实例）
 * 3. 负载均衡（轮询）
 * 4. 自动传递 traceId、userId 等上下文
 * 
 * 配置：
 * rpc:
 *   connectTimeout: 5000
 *   readTimeout: 30000
 *   maxIdleConnections: 100
 *   keepAliveDuration: 300
 * 
 * 使用示例：
 * <pre>{@code
 * RpcClient rpc = app.getPlugin(RpcClient.class);
 * 
 * // 调用服务（自动服务发现）
 * Map<String, Object> result = rpc.call("user-service", "/user/info", Map.of("id", 1));
 * 
 * // 直接调用 URL
 * String response = rpc.post("http://localhost:8081/user/info", Map.of("id", 1));
 * }</pre>
 */
public class RpcClient extends Plugin {
    
    public int connectTimeout = 5000;
    public int readTimeout = 30000;
    public int maxIdleConnections = 100;
    public int keepAliveDuration = 300; // 秒
    
    private OkHttpClient client;
    private DiscoveryPlugin discovery;
    
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    @Override
    public void config() {
        connectTimeout = app.conf.getInt("rpc", "connectTimeout", connectTimeout);
        readTimeout = app.conf.getInt("rpc", "readTimeout", readTimeout);
        maxIdleConnections = app.conf.getInt("rpc", "maxIdleConnections", maxIdleConnections);
        keepAliveDuration = app.conf.getInt("rpc", "keepAliveDuration", keepAliveDuration);
        
        // 创建连接池
        ConnectionPool pool = new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS);
        
        client = new OkHttpClient.Builder()
            .connectionPool(pool)
            .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .build();
        
        app.log.info("[RpcClient] 已初始化，连接池: " + maxIdleConnections + " 连接");
    }
    
    /**
     * 设置服务发现插件
     */
    public RpcClient discovery(DiscoveryPlugin discovery) {
        this.discovery = discovery;
        return this;
    }
    
    // ==================== 服务调用（带服务发现）====================
    
    /**
     * 调用服务（POST JSON）
     * @param serviceName 服务名（如 user-service）
     * @param path 路径（如 /user/info）
     * @param body 请求体
     * @return 响应 Map
     */
    public Map<String, Object> call(String serviceName, String path, Object body) throws IOException {
        String url = resolveUrl(serviceName, path);
        return postJson(url, body);
    }
    
    /**
     * 调用服务（GET）
     */
    public Map<String, Object> callGet(String serviceName, String path) throws IOException {
        String url = resolveUrl(serviceName, path);
        return getJson(url);
    }
    
    private String resolveUrl(String serviceName, String path) throws IOException {
        if (discovery == null) {
            throw new IllegalStateException("未配置服务发现插件，请调用 discovery() 方法设置");
        }
        
        ServiceInstance instance = discovery.getInstance(serviceName);
        if (instance == null) {
            throw new IOException("服务不可用: " + serviceName);
        }
        
        String baseUrl = instance.getUrl();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }
    
    // ==================== 直接 URL 调用 ====================
    
    public String get(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        return execute(request);
    }
    
    public Map<String, Object> getJson(String url) throws IOException {
        String response = get(url);
        return parseJson(response);
    }
    
    public String post(String url, Object body) throws IOException {
        String json = stringify(body);
        RequestBody requestBody = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .build();
        return execute(request);
    }
    
    public Map<String, Object> postJson(String url, Object body) throws IOException {
        String response = post(url, body);
        return parseJson(response);
    }
    
    public String postForm(String url, Map<String, String> form) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder()
            .url(url)
            .post(builder.build())
            .build();
        return execute(request);
    }
    
    // ==================== 带上下文的调用 ====================
    
    /**
     * 带上下文调用（传递 traceId、userId 等）
     */
    public Map<String, Object> callWithContext(String serviceName, String path, Object body,
                                               String traceId, String spanId, Long userId) throws IOException {
        String url = resolveUrl(serviceName, path);
        String json = stringify(body);
        
        Request.Builder builder = new Request.Builder()
            .url(url)
            .post(RequestBody.create(json, JSON));
        
        if (traceId != null) {
            builder.header("X-Trace-Id", traceId);
        }
        if (spanId != null) {
            builder.header("X-Span-Id", spanId);
        }
        if (userId != null) {
            builder.header("X-User-Id", String.valueOf(userId));
        }
        
        String response = execute(builder.build());
        return parseJson(response);
    }
    
    // ==================== 代理方法（Gateway 用）====================
    
    /**
     * 代理请求 - 支持任意 HTTP 方法和原始 body 透传
     * @param method HTTP 方法
     * @param url 目标 URL
     * @param body 原始请求体（可为 null）
     * @param headers 请求头
     * @return ProxyResponse 包含状态码和响应体
     */
    public ProxyResponse proxy(String method, String url, byte[] body, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        
        // 设置请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getValue() != null) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // 设置请求方法和 body
        if ("GET".equalsIgnoreCase(method)) {
            builder.get();
        } else if ("DELETE".equalsIgnoreCase(method)) {
            builder.delete();
        } else if ("HEAD".equalsIgnoreCase(method)) {
            builder.head();
        } else {
            // POST, PUT, PATCH 等需要 body
            MediaType contentType = JSON;
            String ct = headers != null ? headers.get("Content-Type") : null;
            if (ct != null) {
                MediaType parsed = MediaType.parse(ct);
                if (parsed != null) contentType = parsed;
            }
            RequestBody requestBody = body != null && body.length > 0 
                ? RequestBody.create(body, contentType)
                : RequestBody.create(new byte[0], contentType);
            builder.method(method.toUpperCase(), requestBody);
        }
        
        try (Response response = client.newCall(builder.build()).execute()) {
            ResponseBody respBody = response.body();
            return new ProxyResponse(response.code(), respBody != null ? respBody.string() : "");
        }
    }
    
    /**
     * 代理响应
     */
    public static class ProxyResponse {
        public int code;
        public String body;
        
        public ProxyResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
    
    // ==================== 核心执行方法 ====================
    
    private String execute(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }
    
    // ==================== JSON 工具 ====================
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (app != null && app.json != null) {
            return app.json.parseMap(json);
        }
        throw new IllegalStateException("未配置 JsonPlugin");
    }
    
    private String stringify(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        if (app != null && app.json != null) {
            return app.json.stringify(obj);
        }
        throw new IllegalStateException("未配置 JsonPlugin");
    }
    
    @Override
    public void uninstall() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
    
    /**
     * 获取连接池状态（用于监控）
     */
    public String getPoolStats() {
        if (client == null) return "未初始化";
        ConnectionPool pool = client.connectionPool();
        return "空闲连接: " + pool.idleConnectionCount() + ", 总连接: " + pool.connectionCount();
    }
}
