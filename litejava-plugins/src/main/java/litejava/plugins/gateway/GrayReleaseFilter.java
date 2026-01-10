package litejava.plugins.gateway;

import litejava.Context;
import litejava.MiddlewarePlugin;
import litejava.Next;
import litejava.plugins.microservice.DiscoveryPlugin;
import litejava.plugins.microservice.DiscoveryPlugin.ServiceInstance;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 灰度发布过滤器
 * 
 * 支持多种灰度策略：
 * 1. 按比例 - 10% 流量到新版本
 * 2. 按用户 - 指定用户走新版本
 * 3. 按 Header - 带特定 header 走新版本
 * 4. 按 IP - 指定 IP 段走新版本
 * 
 * 配置示例：
 * gray:
 *   enabled: true
 *   rules:
 *     - service: user-service
 *       version: v2
 *       strategy: ratio
 *       ratio: 10           # 10% 流量
 *     - service: order-service
 *       version: v2
 *       strategy: user
 *       users: [1, 2, 100]  # 指定用户
 *     - service: product-service
 *       version: v2
 *       strategy: header
 *       header: X-Gray
 *       value: true
 */
public class GrayReleaseFilter extends MiddlewarePlugin {
    
    public boolean enabled = true;
    public List<GrayRule> rules = new ArrayList<>();
    
    private DiscoveryPlugin discovery;
    
    public GrayReleaseFilter discovery(DiscoveryPlugin discovery) {
        this.discovery = discovery;
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void config() {
        enabled = app.conf.getBool("gray", "enabled", enabled);
        
        Map<String, Object> conf = app.conf.get();
        Object rulesObj = conf.get("gray");
        if (rulesObj instanceof Map) {
            Object rulesList = ((Map<?, ?>) rulesObj).get("rules");
            if (rulesList instanceof List) {
                for (Object item : (List<?>) rulesList) {
                    if (item instanceof Map) {
                        Map<String, Object> ruleMap = (Map<String, Object>) item;
                        GrayRule rule = new GrayRule();
                        rule.service = (String) ruleMap.get("service");
                        rule.version = (String) ruleMap.get("version");
                        rule.strategy = (String) ruleMap.get("strategy");
                        rule.ratio = ruleMap.get("ratio") != null ? 
                            ((Number) ruleMap.get("ratio")).intValue() : 0;
                        rule.users = (List<Long>) ruleMap.get("users");
                        rule.header = (String) ruleMap.get("header");
                        rule.headerValue = (String) ruleMap.get("value");
                        rule.ipPrefix = (String) ruleMap.get("ipPrefix");
                        rules.add(rule);
                    }
                }
            }
        }
        
        app.log.info("[GrayRelease] 已加载 " + rules.size() + " 条灰度规则");
    }
    
    @Override
    public void handle(Context ctx, Next next) throws Exception {
        if (!enabled || rules.isEmpty()) {
            next.run();
            return;
        }
        
        // 解析目标服务（从 path 中提取，如 /api/user-service/xxx）
        String serviceName = extractServiceName(ctx.path);
        if (serviceName != null) {
            GrayRule rule = findRule(serviceName);
            if (rule != null && shouldUseGray(ctx, rule)) {
                // 标记使用灰度版本
                ctx.state.put("grayVersion", rule.version);
                ctx.state.put("grayService", serviceName);
            }
        }
        
        next.run();
    }
    
    /**
     * 根据灰度标记选择实例
     */
    public ServiceInstance selectInstance(Context ctx, String serviceName) {
        if (discovery == null) return null;
        
        String grayVersion = (String) ctx.state.get("grayVersion");
        String grayService = (String) ctx.state.get("grayService");
        
        List<ServiceInstance> instances = discovery.getInstances(serviceName);
        if (instances == null || instances.isEmpty()) return null;
        
        // 如果是灰度服务，优先选择灰度版本实例
        if (serviceName.equals(grayService) && grayVersion != null) {
            for (ServiceInstance inst : instances) {
                if (grayVersion.equals(inst.version)) {
                    return inst;
                }
            }
        }
        
        // 默认轮询
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }
    
    private String extractServiceName(String path) {
        if (path.startsWith("/api/")) {
            String remaining = path.substring(5);
            int slash = remaining.indexOf('/');
            if (slash > 0) {
                return remaining.substring(0, slash);
            }
        }
        return null;
    }
    
    private GrayRule findRule(String serviceName) {
        for (GrayRule rule : rules) {
            if (serviceName.equals(rule.service)) {
                return rule;
            }
        }
        return null;
    }
    
    private boolean shouldUseGray(Context ctx, GrayRule rule) {
        switch (rule.strategy) {
            case "ratio":
                return ThreadLocalRandom.current().nextInt(100) < rule.ratio;
            
            case "user":
                Object userId = ctx.state.get("userId");
                if (userId != null && rule.users != null) {
                    long uid = userId instanceof Number ? 
                        ((Number) userId).longValue() : Long.parseLong(userId.toString());
                    return rule.users.contains(uid);
                }
                return false;
            
            case "header":
                String headerValue = ctx.header(rule.header);
                return rule.headerValue.equals(headerValue);
            
            case "ip":
                String clientIp = ctx.header("X-Real-IP");
                if (clientIp == null) clientIp = ctx.header("X-Forwarded-For");
                return clientIp != null && rule.ipPrefix != null && 
                       clientIp.startsWith(rule.ipPrefix);
            
            default:
                return false;
        }
    }
    
    public static class GrayRule {
        public String service;
        public String version;
        public String strategy;  // ratio / user / header / ip
        public int ratio;
        public List<Long> users;
        public String header;
        public String headerValue;
        public String ipPrefix;
    }
}
