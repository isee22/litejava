package litejava.plugins.microservice;

import litejava.plugins.http.HttpClient;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Nacos 插件 - 服务发现 + 配置中心
 * 
 * 功能：
 * 1. 服务注册/发现（替代 Consul）
 * 2. 配置动态推送
 * 3. 负载均衡
 * 
 * 配置：
 * nacos:
 *   serverAddr: localhost:8848
 *   namespace: public
 *   group: DEFAULT_GROUP
 */
public class NacosPlugin extends DiscoveryPlugin {
    
    public String serverAddr = "localhost:8848";
    public String namespace = "public";
    public String group = "DEFAULT_GROUP";
    public String serviceName;
    public String ip;
    public int port;
    
    private HttpClient http;
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledExecutorService configPollingExecutor;
    private Map<String, List<ServiceInstance>> serviceCache = new ConcurrentHashMap<>();
    private Map<String, List<Consumer<String>>> configListeners = new ConcurrentHashMap<>();
    private Map<String, String> configCache = new ConcurrentHashMap<>();
    
    @Override
    public void config() {
        serverAddr = app.conf.getString("nacos", "serverAddr", serverAddr);
        namespace = app.conf.getString("nacos", "namespace", namespace);
        group = app.conf.getString("nacos", "group", group);
        serviceName = app.conf.getString("server", "name", "unknown");
        port = app.conf.getInt("server", "port", 8080);
        
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            ip = "127.0.0.1";
        }
        
        // 初始化 HTTP 客户端
        http = new HttpClient(app).timeout(5000, 5000);
        
        // 注册服务
        register();
        
        // 心跳线程
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nacos-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(this::heartbeat, 5, 5, TimeUnit.SECONDS);
        
        // 配置轮询线程
        configPollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nacos-config-polling");
            t.setDaemon(true);
            return t;
        });
        configPollingExecutor.scheduleAtFixedRate(this::pollConfigs, 10, 10, TimeUnit.SECONDS);
        
        app.log.info("[Nacos] 已连接: " + serverAddr + ", 服务: " + serviceName);
    }
    
    @Override
    public void register() {
        try {
            String url = "http://" + serverAddr + "/nacos/v1/ns/instance";
            String body = "serviceName=" + HttpClient.encode(serviceName) +
                         "&ip=" + HttpClient.encode(ip) +
                         "&port=" + port +
                         "&namespaceId=" + HttpClient.encode(namespace) +
                         "&groupName=" + HttpClient.encode(group) +
                         "&healthy=true&enabled=true&ephemeral=true";
            http.postForm(url, body);
        } catch (Exception e) {
            app.log.warn("[Nacos] 注册失败: " + e.getMessage());
        }
    }
    
    @Override
    public void deregister() {
        try {
            String url = "http://" + serverAddr + "/nacos/v1/ns/instance" +
                        "?serviceName=" + HttpClient.encode(serviceName) +
                        "&ip=" + HttpClient.encode(ip) +
                        "&port=" + port +
                        "&namespaceId=" + HttpClient.encode(namespace) +
                        "&groupName=" + HttpClient.encode(group);
            http.delete(url);
        } catch (Exception e) {
            // ignore
        }
    }
    
    private void heartbeat() {
        try {
            String url = "http://" + serverAddr + "/nacos/v1/ns/instance/beat";
            String beat = "{\"serviceName\":\"" + serviceName + "\",\"ip\":\"" + ip + "\",\"port\":" + port + "}";
            String body = "serviceName=" + HttpClient.encode(serviceName) +
                         "&ip=" + HttpClient.encode(ip) +
                         "&port=" + port +
                         "&namespaceId=" + HttpClient.encode(namespace) +
                         "&groupName=" + HttpClient.encode(group) +
                         "&beat=" + HttpClient.encode(beat);
            http.put(url, body);
        } catch (Exception e) {
            // 心跳失败，尝试重新注册
            register();
        }
    }
    
    @Override
    public ServiceInstance getInstance(String serviceName) {
        List<ServiceInstance> instances = getInstances(serviceName);
        if (instances.isEmpty()) return null;
        int idx = (int) (System.currentTimeMillis() % instances.size());
        return instances.get(idx);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<ServiceInstance> getInstances(String serviceName) {
        // 先查缓存
        List<ServiceInstance> cached = serviceCache.get(serviceName);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        // 从 Nacos 获取
        try {
            String url = "http://" + serverAddr + "/nacos/v1/ns/instance/list" +
                        "?serviceName=" + HttpClient.encode(serviceName) +
                        "&namespaceId=" + HttpClient.encode(namespace) +
                        "&groupName=" + HttpClient.encode(group) +
                        "&healthyOnly=true";
            Map<String, Object> response = http.getJson(url);
            List<ServiceInstance> instances = parseInstances(response);
            serviceCache.put(serviceName, instances);
            return instances;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<ServiceInstance> parseInstances(Map<String, Object> response) {
        List<ServiceInstance> result = new ArrayList<>();
        Object hosts = response.get("hosts");
        if (!(hosts instanceof List)) return result;
        
        for (Object item : (List<?>) hosts) {
            if (item instanceof Map) {
                Map<String, Object> host = (Map<String, Object>) item;
                ServiceInstance instance = new ServiceInstance();
                instance.host = (String) host.get("ip");
                instance.port = ((Number) host.get("port")).intValue();
                instance.id = instance.host + ":" + instance.port;
                result.add(instance);
            }
        }
        return result;
    }
    
    /**
     * 获取配置
     */
    public String getConfig(String dataId) {
        return getConfig(dataId, group);
    }
    
    public String getConfig(String dataId, String grp) {
        try {
            String url = "http://" + serverAddr + "/nacos/v1/cs/configs" +
                        "?dataId=" + HttpClient.encode(dataId) +
                        "&group=" + HttpClient.encode(grp) +
                        "&tenant=" + HttpClient.encode(namespace);
            String cfg = http.get(url);
            configCache.put(dataId + "@" + grp, cfg);
            return cfg;
        } catch (Exception e) {
            return configCache.get(dataId + "@" + grp);
        }
    }
    
    /**
     * 监听配置变化
     */
    public void addConfigListener(String dataId, Consumer<String> listener) {
        addConfigListener(dataId, group, listener);
    }
    
    public void addConfigListener(String dataId, String grp, Consumer<String> listener) {
        String key = dataId + "@" + grp;
        configListeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
        String cfg = getConfig(dataId, grp);
        if (cfg != null) {
            listener.accept(cfg);
        }
    }
    
    private void pollConfigs() {
        for (String key : configListeners.keySet()) {
            String[] parts = key.split("@");
            String dataId = parts[0];
            String grp = parts.length > 1 ? parts[1] : group;
            
            try {
                String newConfig = getConfig(dataId, grp);
                String oldConfig = configCache.get(key);
                
                if (newConfig != null && !newConfig.equals(oldConfig)) {
                    configCache.put(key, newConfig);
                    List<Consumer<String>> listeners = configListeners.get(key);
                    if (listeners != null) {
                        for (Consumer<String> listener : listeners) {
                            try {
                                listener.accept(newConfig);
                            } catch (Exception e) {
                                app.log.warn("[Nacos] 配置监听器执行失败: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    @Override
    public void uninstall() {
        deregister();
        if (heartbeatExecutor != null) heartbeatExecutor.shutdown();
        if (configPollingExecutor != null) configPollingExecutor.shutdown();
    }
}
