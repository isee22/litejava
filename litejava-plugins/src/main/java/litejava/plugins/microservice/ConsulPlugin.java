package litejava.plugins.microservice;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.health.ServiceHealth;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.cache.KVCache;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Consul 服务发现插件
 * 
 * <p>基于 consul-client (Orbitz) 实现。
 * 
 * <h2>Maven 依赖</h2>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.orbitz.consul</groupId>
 *     <artifactId>consul-client</artifactId>
 *     <version>1.5.3</version>
 * </dependency>
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 基本用法
 * app.use(new ConsulPlugin("user-service", 8080));
 * 
 * // 自定义配置
 * ConsulPlugin consul = new ConsulPlugin("user-service", 8080);
 * consul.consulHost = "consul.example.com";
 * consul.healthCheckPath = "/health";
 * consul.healthCheckInterval = "10s";
 * app.use(consul);
 * }</pre>
 * 
 * <h2>配置文件 (application.yml)</h2>
 * <pre>
 * consul:
 *   host: localhost
 *   port: 8500
 * service:
 *   name: user-service
 *   host: localhost
 *   port: 8080
 *   healthCheckPath: /health
 *   healthCheckInterval: 10s
 * </pre>
 */
public class ConsulPlugin extends DiscoveryPlugin {
    
    /** Consul 服务器地址 */
    public String consulHost = "localhost";
    
    /** Consul 服务器端口 */
    public int consulPort = 8500;
    
    /** 当前服务名称 */
    public String serviceName;
    
    /** 当前服务主机 */
    public String serviceHost = "localhost";
    
    /** 当前服务端口 */
    public int servicePort;
    
    /** 健康检查路径 */
    public String healthCheckPath = "/health";
    
    /** 健康检查间隔 (Consul 格式: "10s", "1m") */
    public String healthCheckInterval = "10s";
    
    /** 服务标签 */
    public List<String> tags = new ArrayList<>();
    
    /** 服务元数据 */
    public Map<String, String> meta = new HashMap<>();
    
    /** Consul 客户端 */
    private Consul consul;
    
    /** 服务 ID */
    private String serviceId;
    
    public ConsulPlugin() {}
    
    public ConsulPlugin(String serviceName, int servicePort) {
        this.serviceName = serviceName;
        this.servicePort = servicePort;
    }
    
    @Override
    public void config() {
        // 从配置文件读取
        consulHost = app.conf.getString("consul", "host", consulHost);
        consulPort = app.conf.getInt("consul", "port", consulPort);
        serviceName = app.conf.getString("service", "name", serviceName);
        serviceHost = app.conf.getString("service", "host", serviceHost);
        servicePort = app.conf.getInt("service", "port", servicePort);
        healthCheckPath = app.conf.getString("service", "healthCheckPath", healthCheckPath);
        healthCheckInterval = app.conf.getString("service", "healthCheckInterval", healthCheckInterval);
        
        // 自动获取容器 IP
        if ("auto".equals(serviceHost)) {
            serviceHost = getContainerIp();
        }
        
        // 初始化 Consul 客户端
        consul = Consul.builder()
            .withHostAndPort(HostAndPort.fromParts(consulHost, consulPort))
            .build();
        
        serviceId = serviceName + "-" + serviceHost + "-" + servicePort;
        
        // 自动注册
        if (serviceName != null && servicePort > 0) {
            register();
        }
    }
    
    /**
     * 获取容器 IP 地址
     */
    private String getContainerIp() {
        try {
            // 方法1: 尝试获取非回环网卡的 IP (优先 eth0)
            java.util.Enumeration<java.net.NetworkInterface> interfaces = 
                java.net.NetworkInterface.getNetworkInterfaces();
            String fallbackIp = null;
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        // 优先返回 eth0 的 IP (Docker 默认网卡)
                        if ("eth0".equals(iface.getName())) {
                            System.out.println("[ConsulPlugin] Auto-detected container IP (eth0): " + ip);
                            return ip;
                        }
                        if (fallbackIp == null) {
                            fallbackIp = ip;
                        }
                    }
                }
            }
            
            if (fallbackIp != null) {
                System.out.println("[ConsulPlugin] Auto-detected container IP: " + fallbackIp);
                return fallbackIp;
            }
            
            // 方法2: 回退到 getLocalHost
            java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
            String ip = addr.getHostAddress();
            System.out.println("[ConsulPlugin] Using localhost IP: " + ip);
            return ip;
        } catch (Exception e) {
            System.out.println("[ConsulPlugin] 获取容器 IP 失败，使用 localhost: " + e.getMessage());
            return "localhost";
        }
    }

    @Override
    public void register() {
        AgentClient agentClient = consul.agentClient();
        
        String healthUrl = "http://" + serviceHost + ":" + servicePort + healthCheckPath;
        
        Registration registration = ImmutableRegistration.builder()
            .id(serviceId)
            .name(serviceName)
            .address(serviceHost)
            .port(servicePort)
            .tags(tags)
            .meta(meta)
            .check(Registration.RegCheck.http(healthUrl, Long.parseLong(
                healthCheckInterval.replaceAll("[^0-9]", ""))))
            .build();
        
        agentClient.register(registration);
        app.log.info("Registered service: " + serviceName + " at " + serviceHost + ":" + servicePort);
    }
    
    @Override
    public void deregister() {
        if (consul != null && serviceId != null) {
            try {
                consul.agentClient().deregister(serviceId);
                app.log.info("Deregistered service: " + serviceId);
            } catch (Exception e) {
                app.log.warn("Failed to deregister service: " + e.getMessage());
            }
        }
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        HealthClient healthClient = consul.healthClient();
        
        List<ServiceHealth> nodes = healthClient
            .getHealthyServiceInstances(serviceName)
            .getResponse();
        
        return nodes.stream()
            .map(node -> {
                com.orbitz.consul.model.health.Service service = node.getService();
                ServiceInstance instance = new ServiceInstance(
                    service.getId(),
                    serviceName,
                    service.getAddress(),
                    service.getPort()
                );
                instance.metadata = service.getMeta();
                return instance;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有已注册的服务及其实例
     * 用于网关透明路由
     */
    @Override
    public Map<String, List<ServiceInstance>> getAllServices() {
        Map<String, List<ServiceInstance>> result = new HashMap<>();
        
        try {
            // 获取所有服务名
            Map<String, List<String>> services = 
                consul.catalogClient().getServices().getResponse();
            
            for (String svcName : services.keySet()) {
                // 跳过 consul 自身
                if ("consul".equals(svcName)) continue;
                
                List<ServiceInstance> instances = getInstances(svcName);
                if (!instances.isEmpty()) {
                    result.put(svcName, instances);
                }
            }
        } catch (Exception e) {
            app.log.warn("获取所有服务失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取原生 Consul 客户端（高级用法）
     */
    public Consul getConsul() {
        return consul;
    }
    
    // ==================== KV 配置管理 ====================
    
    /** 配置缓存 */
    private Map<String, String> configCache = new ConcurrentHashMap<>();
    
    /** 配置监听器 */
    private Map<String, KVCache> watchers = new ConcurrentHashMap<>();
    
    /**
     * 获取配置值
     * 
     * @param key 配置键 (如 "config/user-service/database.url")
     * @return 配置值，不存在返回 null
     */
    public String getConfig(String key) {
        // 先查缓存
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }
        
        KeyValueClient kvClient = consul.keyValueClient();
        Optional<Value> value = kvClient.getValue(key);
        
        if (value.isPresent() && value.get().getValueAsString().isPresent()) {
            String val = value.get().getValueAsString().get();
            configCache.put(key, val);
            return val;
        }
        return null;
    }
    
    /**
     * 获取配置值（带默认值）
     */
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 设置配置值
     */
    public void setConfig(String key, String value) {
        consul.keyValueClient().putValue(key, value);
        configCache.put(key, value);
    }
    
    /**
     * 删除配置
     */
    public void deleteConfig(String key) {
        consul.keyValueClient().deleteKey(key);
        configCache.remove(key);
    }
    
    /**
     * 获取指定前缀下的所有配置
     * 
     * @param prefix 前缀 (如 "config/user-service/")
     * @return 配置 Map
     */
    public Map<String, String> getConfigsByPrefix(String prefix) {
        KeyValueClient kvClient = consul.keyValueClient();
        List<Value> values = kvClient.getValues(prefix);
        
        Map<String, String> result = new HashMap<>();
        for (Value value : values) {
            if (value.getValueAsString().isPresent()) {
                result.put(value.getKey(), value.getValueAsString().get());
            }
        }
        return result;
    }
    
    /**
     * 监听配置变化
     * 
     * @param key 配置键
     * @param listener 变化回调 (newValue)
     */
    public void watchConfig(String key, Consumer<String> listener) {
        KeyValueClient kvClient = consul.keyValueClient();
        KVCache cache = KVCache.newCache(kvClient, key);
        
        cache.addListener(newValues -> {
            Optional<Value> value = newValues.values().stream().findFirst();
            if (value.isPresent() && value.get().getValueAsString().isPresent()) {
                String newVal = value.get().getValueAsString().get();
                String oldVal = configCache.get(key);
                if (!newVal.equals(oldVal)) {
                    configCache.put(key, newVal);
                    listener.accept(newVal);
                }
            }
        });
        
        cache.start();
        watchers.put(key, cache);
    }
    
    /**
     * 停止监听配置
     */
    public void unwatchConfig(String key) {
        KVCache cache = watchers.remove(key);
        if (cache != null) {
            cache.stop();
        }
    }
    
    /**
     * 加载服务配置到 app.conf
     * 
     * <p>从 Consul KV 加载 config/{serviceName}/ 下的配置，
     * 并合并到 app.conf 中。
     * 
     * @param watch 是否监听变化自动更新
     */
    public void loadServiceConfig(boolean watch) {
        String prefix = "config/" + serviceName + "/";
        Map<String, String> configs = getConfigsByPrefix(prefix);
        
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey().substring(prefix.length());
            app.conf.set(key, entry.getValue());
        }
        
        if (watch) {
            watchConfig(prefix, newValue -> {
                // 重新加载配置
                Map<String, String> newConfigs = getConfigsByPrefix(prefix);
                for (Map.Entry<String, String> entry : newConfigs.entrySet()) {
                    String key = entry.getKey().substring(prefix.length());
                    app.conf.set(key, entry.getValue());
                }
                app.log.info("Config reloaded from Consul");
            });
        }
    }
    
    @Override
    public void uninstall() {
        // 停止所有监听
        for (KVCache cache : watchers.values()) {
            cache.stop();
        }
        watchers.clear();
        
        // 注销服务
        deregister();
    }
}
