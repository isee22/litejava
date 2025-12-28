package litejava.plugins.discovery;

import litejava.Plugin;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.health.ServiceHealth;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Consul 服务发现插件
 * 
 * <h2>配置</h2>
 * <pre>
 * consul:
 *   host: localhost
 *   port: 8500
 *   service:
 *     name: my-service
 *     port: 8080
 *     tags: [web, api]
 *     checkInterval: 10s
 * </pre>
 */
public class ConsulPlugin extends Plugin {
    
    public String host = "localhost";
    public int port = 8500;
    public String serviceName;
    public int servicePort = 8080;
    public String[] tags = {};
    public String checkInterval = "10s";
    
    public Consul consul;
    public String serviceId;
    
    public ConsulPlugin() {
    }
    
    /**
     * 构造函数 - 指定主机
     */
    public ConsulPlugin(String host) {
        this.host = host;
    }
    
    /**
     * 构造函数 - 指定主机和端口
     */
    public ConsulPlugin(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 构造函数 - 指定主机、端口和服务名
     */
    public ConsulPlugin(String host, int port, String serviceName, int servicePort) {
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.servicePort = servicePort;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        host = app.conf.getString("consul", "host", host);
        port = app.conf.getInt("consul", "port", port);
        serviceName = app.conf.getString("consul.service", "name", serviceName);
        servicePort = app.conf.getInt("consul.service", "port", servicePort);
        checkInterval = app.conf.getString("consul.service", "checkInterval", checkInterval);
        
        String tagsStr = app.conf.getString("consul.service", "tags", null);
        if (tagsStr != null) {
            tags = tagsStr.split(",");
        }
        
        // 连接 Consul
        consul = Consul.builder()
            .withUrl("http://" + host + ":" + port)
            .build();
        
        // 注册服务
        if (serviceName != null && !serviceName.isEmpty()) {
            registerService();
        }
        
        app.log.info("ConsulPlugin: Connected to " + host + ":" + port);
    }
    
    private void registerService() {
        AgentClient agent = consul.agentClient();
        serviceId = serviceName + "-" + servicePort;
        
        Registration.RegCheck check = Registration.RegCheck.http(
            "http://localhost:" + servicePort + "/health",
            Long.parseLong(checkInterval.replace("s", ""))
        );
        
        Registration registration = ImmutableRegistration.builder()
            .id(serviceId)
            .name(serviceName)
            .port(servicePort)
            .check(check)
            .addTags(tags)
            .build();
        
        agent.register(registration);
        app.log.info("ConsulPlugin: Registered service " + serviceId);
    }
    
    public List<String> getHealthyInstances(String serviceName) {
        HealthClient health = consul.healthClient();
        List<ServiceHealth> nodes = health.getHealthyServiceInstances(serviceName).getResponse();
        
        return nodes.stream()
            .map(node -> node.getService().getAddress() + ":" + node.getService().getPort())
            .collect(Collectors.toList());
    }
    
    public String getOneInstance(String serviceName) {
        List<String> instances = getHealthyInstances(serviceName);
        if (instances.isEmpty()) {
            return null;
        }
        // 简单轮询
        return instances.get((int) (System.currentTimeMillis() % instances.size()));
    }
    
    @Override
    public void uninstall() {
        if (consul != null && serviceId != null) {
            try {
                consul.agentClient().deregister(serviceId);
                app.log.info("ConsulPlugin: Deregistered service " + serviceId);
            } catch (Exception ignored) {}
        }
    }
}
