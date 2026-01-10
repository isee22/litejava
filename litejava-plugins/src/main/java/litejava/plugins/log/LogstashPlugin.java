package litejava.plugins.log;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 日志聚合插件 - 发送到 ELK (Elasticsearch/Logstash)
 * 
 * 支持两种模式：
 * 1. 直接发送到 Elasticsearch
 * 2. 发送到 Logstash HTTP Input
 * 
 * 配置示例：
 * elk:
 *   enabled: true
 *   mode: elasticsearch    # elasticsearch / logstash
 *   url: http://localhost:9200
 *   index: litejava-logs
 *   username: elastic
 *   password: changeme
 *   batchSize: 100
 *   flushInterval: 5000
 * 
 * 日志格式：
 * {
 *   "@timestamp": "2024-01-01T12:00:00Z",
 *   "service": "user-service",
 *   "traceId": "abc123",
 *   "method": "POST",
 *   "path": "/user/create",
 *   "status": 200,
 *   "duration": 50,
 *   "userId": 1,
 *   "clientIp": "192.168.1.1"
 * }
 */
public class LogstashPlugin extends MiddlewarePlugin {
    
    public boolean enabled = true;
    public String mode = "elasticsearch";
    public String url = "http://localhost:9200";
    public String index = "litejava-logs";
    public String username = null;
    public String password = null;
    public int batchSize = 100;
    public int flushInterval = 5000;
    
    private String serviceName;
    private BlockingQueue<Map<String, Object>> logQueue;
    private Thread flushThread;
    private volatile boolean running = true;
    
    @Override
    public void config() {
        enabled = app.conf.getBool("elk", "enabled", enabled);
        mode = app.conf.getString("elk", "mode", mode);
        url = app.conf.getString("elk", "url", url);
        index = app.conf.getString("elk", "index", index);
        username = app.conf.getString("elk", "username", null);
        password = app.conf.getString("elk", "password", null);
        batchSize = app.conf.getInt("elk", "batchSize", batchSize);
        flushInterval = app.conf.getInt("elk", "flushInterval", flushInterval);
        serviceName = app.conf.getString("service", "name", "unknown");
        
        if (!enabled) return;
        
        logQueue = new LinkedBlockingQueue<>(10000);
        
        // 启动异步刷新线程
        flushThread = new Thread(this::flushLoop, "elk-flush");
        flushThread.setDaemon(true);
        flushThread.start();
        
        app.log.info("[ELK] 日志聚合已启用，目标: " + url + "/" + index);
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        if (!enabled) {
            next.run();
            return;
        }
        
        long start = System.currentTimeMillis();
        
        try {
            next.run();
        } finally {
            long duration = System.currentTimeMillis() - start;
            
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("@timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            log.put("service", serviceName);
            log.put("method", ctx.method);
            log.put("path", ctx.path);
            log.put("status", ctx.getResponseStatus());
            log.put("duration", duration);
            
            // 链路追踪
            Object traceId = ctx.state.get("traceId");
            if (traceId != null) log.put("traceId", traceId);
            
            Object spanId = ctx.state.get("spanId");
            if (spanId != null) log.put("spanId", spanId);
            
            // 用户信息
            Object userId = ctx.state.get("userId");
            if (userId != null) log.put("userId", userId);
            
            // 客户端 IP
            String clientIp = ctx.header("X-Real-IP");
            if (clientIp == null) clientIp = ctx.header("X-Forwarded-For");
            if (clientIp != null) log.put("clientIp", clientIp);
            
            // 错误信息
            Object error = ctx.state.get("error");
            if (error != null) log.put("error", error.toString());
            
            logQueue.offer(log);
        }
    }
    
    /**
     * 手动记录日志
     */
    public void log(String level, String message, Map<String, Object> extra) {
        if (!enabled) return;
        
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("@timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        log.put("service", serviceName);
        log.put("level", level);
        log.put("message", message);
        if (extra != null) log.putAll(extra);
        
        logQueue.offer(log);
    }
    
    public void info(String message) {
        log("INFO", message, null);
    }
    
    public void warn(String message) {
        log("WARN", message, null);
    }
    
    public void error(String message, Throwable e) {
        log("ERROR", message, Map.of("exception", e.toString()));
    }
    
    private void flushLoop() {
        while (running) {
            try {
                Thread.sleep(flushInterval);
                flush();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void flush() {
        if (logQueue.isEmpty()) return;
        
        StringBuilder bulk = new StringBuilder();
        int count = 0;
        
        Map<String, Object> log;
        while ((log = logQueue.poll()) != null && count < batchSize) {
            if ("elasticsearch".equals(mode)) {
                // Elasticsearch bulk format
                bulk.append("{\"index\":{\"_index\":\"").append(index).append("\"}}\n");
                bulk.append(app.json.stringify(log)).append("\n");
            } else {
                // Logstash JSON lines
                bulk.append(app.json.stringify(log)).append("\n");
            }
            count++;
        }
        
        if (count == 0) return;
        
        try {
            String endpoint = "elasticsearch".equals(mode) 
                ? url + "/_bulk" 
                : url;
            
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", 
                "elasticsearch".equals(mode) ? "application/x-ndjson" : "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            
            // Basic Auth
            if (username != null && password != null) {
                String auth = java.util.Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
                conn.setRequestProperty("Authorization", "Basic " + auth);
            }
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bulk.toString().getBytes(StandardCharsets.UTF_8));
            }
            
            int code = conn.getResponseCode();
            if (code >= 400) {
                app.log.error("[ELK] 发送失败: HTTP " + code);
            }
        } catch (Exception e) {
            app.log.error("[ELK] 发送失败: " + e.getMessage());
        }
    }
    
    @Override
    public void uninstall() {
        running = false;
        if (flushThread != null) {
            flushThread.interrupt();
        }
        flush(); // 最后刷新一次
    }
}
