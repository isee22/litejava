package litejava.plugins.discovery;

import litejava.Plugin;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Zookeeper 插件 - 服务注册与发现、分布式协调
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * zookeeper:
 *   connectString: localhost:2181
 *   namespace: litejava
 *   sessionTimeout: 60000
 *   connectionTimeout: 15000
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new ZookeeperPlugin());
 * 
 * // 注册服务
 * ZookeeperPlugin.instance.registerService("user-service", "localhost", 8080);
 * 
 * // 发现服务
 * List<ZookeeperPlugin.Instance> instances = ZookeeperPlugin.instance.getInstances("user-service");
 * 
 * // 监听节点变化
 * ZookeeperPlugin.instance.watch("/config", event -> {
 *     System.out.println("Node changed: " + event);
 * });
 * }</pre>
 */
public class ZookeeperPlugin extends Plugin {
    
    public static ZookeeperPlugin instance;
    
    /** Curator 客户端 */
    public CuratorFramework client;
    
    /** 服务发现 */
    public ServiceDiscovery<String> serviceDiscovery;
    
    /** 连接字符串 */
    public String connectString = "localhost:2181";
    
    /** 命名空间 */
    public String namespace = "litejava";
    
    /** Session 超时（毫秒） */
    public int sessionTimeout = 60000;
    
    /** 连接超时（毫秒） */
    public int connectionTimeout = 15000;
    
    /** 服务基础路径 */
    public String basePath = "/services";
    
    public static class Instance {
        public String name;
        public String id;
        public String address;
        public int port;
        public String payload;
    }
    
    @Override
    public void config() {
        instance = this;
        
        connectString = app.conf.getString("zookeeper", "connectString", connectString);
        namespace = app.conf.getString("zookeeper", "namespace", namespace);
        sessionTimeout = app.conf.getInt("zookeeper", "sessionTimeout", sessionTimeout);
        connectionTimeout = app.conf.getInt("zookeeper", "connectionTimeout", connectionTimeout);
        basePath = app.conf.getString("zookeeper", "basePath", basePath);
        
        client = CuratorFrameworkFactory.builder()
            .connectString(connectString)
            .namespace(namespace)
            .sessionTimeoutMs(sessionTimeout)
            .connectionTimeoutMs(connectionTimeout)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
        
        client.start();
        
        try {
            serviceDiscovery = ServiceDiscoveryBuilder.builder(String.class)
                .client(client)
                .basePath(basePath)
                .serializer(new JsonInstanceSerializer<>(String.class))
                .build();
            serviceDiscovery.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start service discovery", e);
        }
        
        app.log.info("ZookeeperPlugin: Connected to " + connectString);
    }
    
    /**
     * 注册服务
     */
    public void registerService(String name, String address, int port) {
        registerService(name, address, port, null);
    }
    
    /**
     * 注册服务（带 payload）
     */
    public void registerService(String name, String address, int port, String payload) {
        try {
            ServiceInstance<String> instance = ServiceInstance.<String>builder()
                .name(name)
                .address(address)
                .port(port)
                .payload(payload)
                .build();
            serviceDiscovery.registerService(instance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register service", e);
        }
    }
    
    /**
     * 获取服务实例列表
     */
    public List<Instance> getInstances(String name) {
        List<Instance> result = new ArrayList<>();
        try {
            Collection<ServiceInstance<String>> instances = serviceDiscovery.queryForInstances(name);
            for (ServiceInstance<String> si : instances) {
                Instance inst = new Instance();
                inst.name = si.getName();
                inst.id = si.getId();
                inst.address = si.getAddress();
                inst.port = si.getPort();
                inst.payload = si.getPayload();
                result.add(inst);
            }
        } catch (Exception e) {
            app.log.error("Failed to get instances: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * 创建节点
     */
    public void create(String path, String data) {
        try {
            client.create().creatingParentsIfNeeded().forPath(path, data.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create node", e);
        }
    }
    
    /**
     * 获取节点数据
     */
    public String get(String path) {
        try {
            byte[] data = client.getData().forPath(path);
            return data != null ? new String(data) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 设置节点数据
     */
    public void set(String path, String data) {
        try {
            client.setData().forPath(path, data.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set node data", e);
        }
    }
    
    /**
     * 删除节点
     */
    public void delete(String path) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete node", e);
        }
    }
    
    /**
     * 监听子节点变化
     */
    public void watch(String path, Consumer<PathChildrenCacheEvent> listener) {
        try {
            PathChildrenCache cache = new PathChildrenCache(client, path, true);
            cache.getListenable().addListener((client, event) -> listener.accept(event));
            cache.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to watch path", e);
        }
    }
    
    @Override
    public void uninstall() {
        try {
            if (serviceDiscovery != null) {
                serviceDiscovery.close();
            }
        } catch (Exception ignored) {}
        
        if (client != null) {
            client.close();
        }
        instance = null;
    }
}
