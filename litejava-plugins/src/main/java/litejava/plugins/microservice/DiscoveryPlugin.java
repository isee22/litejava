package litejava.plugins.microservice;

import litejava.Plugin;

import java.util.List;
import java.util.Map;

/**
 * 服务发现插件基类
 * 
 * <p>定义服务注册/发现的标准接口，具体实现由子类提供：
 * <ul>
 *   <li>ConsulPlugin - Consul 实现 (consul-client)</li>
 *   <li>NacosPlugin - Nacos 实现 (nacos-client)</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 使用 Consul
 * app.use(new ConsulPlugin("user-service", 8080));
 * 
 * // 获取服务实例
 * DiscoveryPlugin discovery = app.getPlugin(ConsulPlugin.class);
 * ServiceInstance instance = discovery.getInstance("order-service");
 * }</pre>
 */
public abstract class DiscoveryPlugin extends Plugin {
    
    /**
     * 注册当前服务
     */
    public abstract void register();
    
    /**
     * 注销当前服务
     */
    public abstract void deregister();
    
    /**
     * 获取服务的所有健康实例
     * @param serviceName 服务名称
     * @return 服务实例列表
     */
    public abstract List<ServiceInstance> getInstances(String serviceName);
    
    /**
     * 获取所有已注册的服务及其实例
     * 用于网关透明路由
     * @return 服务名 → 实例列表 的映射
     */
    public Map<String, List<ServiceInstance>> getAllServices() {
        return java.util.Collections.emptyMap();
    }
    
    /**
     * 获取服务的一个实例（简单轮询）
     * @param serviceName 服务名称
     * @return 服务实例，无可用实例返回 null
     */
    public ServiceInstance getInstance(String serviceName) {
        List<ServiceInstance> instances = getInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int index = (int) (System.currentTimeMillis() % instances.size());
        return instances.get(index);
    }
    
    @Override
    public void uninstall() {
        deregister();
    }
    
    /**
     * 服务实例
     */
    public static class ServiceInstance {
        public String id;
        public String serviceName;
        public String host;
        public int port;
        public String version;  // 版本号，用于灰度发布
        public Map<String, String> metadata;
        
        public ServiceInstance() {}
        
        public ServiceInstance(String id, String serviceName, String host, int port) {
            this.id = id;
            this.serviceName = serviceName;
            this.host = host;
            this.port = port;
        }
        
        public String getUrl() {
            return "http://" + host + ":" + port;
        }
        
        public Map<String, String> getMeta() {
            return metadata;
        }
    }
}
