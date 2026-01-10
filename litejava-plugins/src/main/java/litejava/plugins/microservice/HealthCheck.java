package litejava.plugins.microservice;

import litejava.Plugin;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 微服务健康检查插件
 * 
 * <p>支持自定义健康检查指标，适配 Consul/Nacos/K8s 健康探测。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * HealthCheck health = new HealthCheck();
 * 
 * // 添加自定义检查
 * health.addCheck("db", () -> db.isConnected());
 * health.addCheck("redis", () -> redis.ping());
 * health.addCheck("diskSpace", () -> {
 *     long free = new File("/").getFreeSpace();
 *     return free > 1024 * 1024 * 100; // > 100MB
 * });
 * 
 * app.use(health);
 * }</pre>
 * 
 * <h2>响应示例</h2>
 * <pre>{@code
 * // 全部健康
 * GET /health -> 200
 * {
 *   "status": "UP",
 *   "components": {
 *     "db": { "status": "UP" },
 *     "redis": { "status": "UP" }
 *   }
 * }
 * 
 * // 部分不健康
 * GET /health -> 503
 * {
 *   "status": "DOWN",
 *   "components": {
 *     "db": { "status": "UP" },
 *     "redis": { "status": "DOWN", "error": "Connection refused" }
 *   }
 * }
 * }</pre>
 * 
 * <h2>K8s 探针配置</h2>
 * <pre>
 * livenessProbe:
 *   httpGet:
 *     path: /health/live
 *     port: 8080
 * readinessProbe:
 *   httpGet:
 *     path: /health/ready
 *     port: 8080
 * </pre>
 */
public class HealthCheck extends Plugin {
    
    /** 健康检查路径 */
    public String path = "/health";
    
    /** 检查超时时间（毫秒） */
    public long timeout = 3000;
    
    /** 存活检查（liveness）- 失败则重启 */
    private final Map<String, Supplier<Boolean>> livenessChecks = new LinkedHashMap<>();
    
    /** 就绪检查（readiness）- 失败则不接流量 */
    private final Map<String, Supplier<Boolean>> readinessChecks = new LinkedHashMap<>();
    
    /** 通用检查（同时用于 liveness 和 readiness） */
    private final Map<String, Supplier<Boolean>> checks = new LinkedHashMap<>();
    
    /** 带详情的检查 */
    private final Map<String, Supplier<CheckResult>> detailedChecks = new LinkedHashMap<>();
    
    public HealthCheck() {}
    
    public HealthCheck(String path) {
        this.path = path;
    }
    
    /**
     * 添加健康检查
     * @param name 检查名称
     * @param check 检查函数，返回 true 表示健康
     */
    public HealthCheck addCheck(String name, Supplier<Boolean> check) {
        checks.put(name, check);
        return this;
    }
    
    /**
     * 添加带详情的健康检查
     */
    public HealthCheck addCheck(String name, Supplier<CheckResult> check, boolean detailed) {
        if (detailed) {
            detailedChecks.put(name, check);
        }
        return this;
    }
    
    /**
     * 添加存活检查（K8s liveness）
     */
    public HealthCheck addLivenessCheck(String name, Supplier<Boolean> check) {
        livenessChecks.put(name, check);
        return this;
    }
    
    /**
     * 添加就绪检查（K8s readiness）
     */
    public HealthCheck addReadinessCheck(String name, Supplier<Boolean> check) {
        readinessChecks.put(name, check);
        return this;
    }
    
    @Override
    public void config() {
        path = app.conf.getString("health", "path", path);
        timeout = app.conf.getLong("health", "timeout", timeout);
        
        // 主健康检查端点
        app.get(path, ctx -> {
            HealthResult result = checkAll();
            ctx.status(result.healthy ? 200 : 503);
            ctx.json(result.toMap());
        });
        
        // K8s liveness 探针
        app.get(path + "/live", ctx -> {
            HealthResult result = checkLiveness();
            ctx.status(result.healthy ? 200 : 503);
            ctx.json(result.toMap());
        });
        
        // K8s readiness 探针
        app.get(path + "/ready", ctx -> {
            HealthResult result = checkReadiness();
            ctx.status(result.healthy ? 200 : 503);
            ctx.json(result.toMap());
        });
    }
    
    /**
     * 执行所有检查
     */
    public HealthResult checkAll() {
        Map<String, Supplier<Boolean>> allChecks = new LinkedHashMap<>();
        allChecks.putAll(checks);
        allChecks.putAll(livenessChecks);
        allChecks.putAll(readinessChecks);
        return doCheck(allChecks);
    }
    
    /**
     * 执行存活检查
     */
    public HealthResult checkLiveness() {
        Map<String, Supplier<Boolean>> allChecks = new LinkedHashMap<>();
        allChecks.putAll(checks);
        allChecks.putAll(livenessChecks);
        return doCheck(allChecks);
    }
    
    /**
     * 执行就绪检查
     */
    public HealthResult checkReadiness() {
        Map<String, Supplier<Boolean>> allChecks = new LinkedHashMap<>();
        allChecks.putAll(checks);
        allChecks.putAll(readinessChecks);
        return doCheck(allChecks);
    }
    
    private HealthResult doCheck(Map<String, Supplier<Boolean>> checksToRun) {
        HealthResult result = new HealthResult();
        result.healthy = true;
        
        ExecutorService executor = Executors.newCachedThreadPool();
        
        try {
            // 执行简单检查
            for (Map.Entry<String, Supplier<Boolean>> entry : checksToRun.entrySet()) {
                String name = entry.getKey();
                Supplier<Boolean> check = entry.getValue();
                
                Future<CheckResult> future = executor.submit(() -> {
                    try {
                        boolean healthy = check.get();
                        return new CheckResult(healthy);
                    } catch (Exception e) {
                        return new CheckResult(false, e.getMessage());
                    }
                });
                
                try {
                    CheckResult checkResult = future.get(timeout, TimeUnit.MILLISECONDS);
                    result.components.put(name, checkResult);
                    if (!checkResult.healthy) {
                        result.healthy = false;
                    }
                } catch (TimeoutException e) {
                    result.components.put(name, new CheckResult(false, "Timeout"));
                    result.healthy = false;
                    future.cancel(true);
                } catch (Exception e) {
                    result.components.put(name, new CheckResult(false, e.getMessage()));
                    result.healthy = false;
                }
            }
            
            // 执行带详情的检查
            for (Map.Entry<String, Supplier<CheckResult>> entry : detailedChecks.entrySet()) {
                String name = entry.getKey();
                Supplier<CheckResult> check = entry.getValue();
                
                Future<CheckResult> future = executor.submit(check::get);
                
                try {
                    CheckResult checkResult = future.get(timeout, TimeUnit.MILLISECONDS);
                    result.components.put(name, checkResult);
                    if (!checkResult.healthy) {
                        result.healthy = false;
                    }
                } catch (TimeoutException e) {
                    result.components.put(name, new CheckResult(false, "Timeout"));
                    result.healthy = false;
                    future.cancel(true);
                } catch (Exception e) {
                    result.components.put(name, new CheckResult(false, e.getMessage()));
                    result.healthy = false;
                }
            }
        } finally {
            executor.shutdownNow();
        }
        
        return result;
    }
    
    /**
     * 健康检查结果
     */
    public static class HealthResult {
        public boolean healthy;
        public Map<String, CheckResult> components = new LinkedHashMap<>();
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", healthy ? "UP" : "DOWN");
            
            if (!components.isEmpty()) {
                Map<String, Object> comps = new LinkedHashMap<>();
                for (Map.Entry<String, CheckResult> entry : components.entrySet()) {
                    comps.put(entry.getKey(), entry.getValue().toMap());
                }
                map.put("components", comps);
            }
            
            return map;
        }
    }
    
    /**
     * 单项检查结果
     */
    public static class CheckResult {
        public boolean healthy;
        public String error;
        public Map<String, Object> details;
        
        public CheckResult(boolean healthy) {
            this.healthy = healthy;
        }
        
        public CheckResult(boolean healthy, String error) {
            this.healthy = healthy;
            this.error = error;
        }
        
        public CheckResult(boolean healthy, Map<String, Object> details) {
            this.healthy = healthy;
            this.details = details;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", healthy ? "UP" : "DOWN");
            if (error != null) {
                map.put("error", error);
            }
            if (details != null) {
                map.putAll(details);
            }
            return map;
        }
    }
}
