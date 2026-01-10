package litejava.plugins.http;

import litejava.App;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 简单 HTTP 客户端工具类
 * 
 * 用于插件内部的 HTTP 调用，如：
 * - 服务注册/发现（Consul、Nacos）
 * - 链路追踪上报（Zipkin）
 * - 配置中心拉取
 * 
 * 连接复用说明：
 * - HttpURLConnection 底层通过 keep-alive 自动复用 TCP 连接
 * - JVM 默认保持 5 个空闲连接，每个连接存活 5 秒
 * - 可通过系统属性调整：http.maxConnections, http.keepAlive
 * 
 * 如需更高性能，可使用 OkHttpClient 替代（需添加依赖）
 * 
 * 使用示例：
 * <pre>{@code
 * HttpClient http = new HttpClient(app);
 * 
 * // GET 请求
 * String result = http.get("http://localhost:8500/v1/health");
 * 
 * // POST JSON
 * Map<String, Object> body = Map.of("name", "test");
 * String result = http.postJson("http://localhost:8080/api", body);
 * }</pre>
 */
public class HttpClient {
    
    public App app;
    public int connectTimeout = 5000;
    public int readTimeout = 10000;
    
    public HttpClient() {}
    
    public HttpClient(App app) {
        this.app = app;
    }
    
    public HttpClient timeout(int connectMs, int readMs) {
        this.connectTimeout = connectMs;
        this.readTimeout = readMs;
        return this;
    }
    
    // ==================== GET ====================
    
    public String get(String url) throws IOException {
        return request("GET", url, null, null);
    }
    
    public Map<String, Object> getJson(String url) throws IOException {
        String response = get(url);
        return parseJson(response);
    }
    
    // ==================== POST ====================
    
    public String postJson(String url, Object body) throws IOException {
        String json = stringify(body);
        return request("POST", url, json, "application/json");
    }
    
    public String postForm(String url, String formData) throws IOException {
        return request("POST", url, formData, "application/x-www-form-urlencoded");
    }
    
    public Map<String, Object> postJsonGetJson(String url, Object body) throws IOException {
        String response = postJson(url, body);
        return parseJson(response);
    }
    
    // ==================== PUT ====================
    
    public String put(String url, String body) throws IOException {
        return request("PUT", url, body, "application/x-www-form-urlencoded");
    }
    
    public String putJson(String url, Object body) throws IOException {
        String json = stringify(body);
        return request("PUT", url, json, "application/json");
    }
    
    // ==================== DELETE ====================
    
    public String delete(String url) throws IOException {
        return request("DELETE", url, null, null);
    }
    
    // ==================== 核心请求方法 ====================
    
    public String request(String method, String url, String body, String contentType) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        
        if (contentType != null) {
            conn.setRequestProperty("Content-Type", contentType);
        }
        conn.setRequestProperty("Accept", "application/json");
        
        // 写入请求体
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        // 读取响应
        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
        
        if (is == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
    
    // ==================== JSON 工具方法 ====================
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseJson(String json) {
        if (app != null && app.json != null) {
            return app.json.parseMap(json);
        }
        // 简单解析（fallback）
        return simpleParseMap(json);
    }
    
    public String stringify(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        if (app != null && app.json != null) {
            return app.json.stringify(obj);
        }
        // 简单序列化（fallback）
        if (obj instanceof Map) {
            return simpleStringifyMap((Map<?, ?>) obj);
        }
        return obj.toString();
    }
    
    // ==================== 简单 JSON 实现（无依赖 fallback）====================
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> simpleParseMap(String json) {
        // 极简实现，只支持简单 JSON
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        if (json == null || json.isEmpty()) return map;
        
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return map;
        
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return map;
        
        // 简单分割（不处理嵌套）
        int depth = 0;
        int start = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                parseKeyValue(json.substring(start, i).trim(), map);
                start = i + 1;
            }
        }
        if (start < json.length()) {
            parseKeyValue(json.substring(start).trim(), map);
        }
        return map;
    }
    
    private void parseKeyValue(String kv, Map<String, Object> map) {
        int colon = kv.indexOf(':');
        if (colon < 0) return;
        
        String key = kv.substring(0, colon).trim();
        String value = kv.substring(colon + 1).trim();
        
        // 去掉引号
        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }
        
        // 解析值
        if (value.equals("null")) {
            map.put(key, null);
        } else if (value.equals("true")) {
            map.put(key, true);
        } else if (value.equals("false")) {
            map.put(key, false);
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            map.put(key, value.substring(1, value.length() - 1));
        } else if (value.matches("-?\\d+")) {
            map.put(key, Long.parseLong(value));
        } else if (value.matches("-?\\d+\\.\\d+")) {
            map.put(key, Double.parseDouble(value));
        } else {
            map.put(key, value);
        }
    }
    
    private String simpleStringifyMap(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object v = entry.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else if (v instanceof Map) sb.append(simpleStringifyMap((Map<?, ?>) v));
            else sb.append("\"").append(v.toString().replace("\"", "\\\"")).append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }
    
    // ==================== URL 编码 ====================
    
    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
