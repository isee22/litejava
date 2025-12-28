package litejava.plugins.discovery;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import litejava.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Eureka 服务发现插件 - 基于 Netflix Eureka Client
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * eureka:
 *   serviceUrl: http://localhost:8761/eureka/
 *   appName: my-service
 *   port: 8080
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new EurekaPlugin());
 * 
 * // 获取服务实例
 * List<ServiceInstance> instances = EurekaPlugin.instance.getInstances("user-service");
 * }</pre>
 */
public class EurekaPlugin extends Plugin {
    
    public static EurekaPlugin instance;
    
    /** Eureka 客户端 */
    public EurekaClient eurekaClient;
    
    /** ApplicationInfoManager */
    public ApplicationInfoManager applicationInfoManager;
    
    /** Eureka Server URL */
    public String serviceUrl = "http://localhost:8761/eureka/";
    
    /** 应用名称 */
    public String appName;
    
    /** 服务端口 */
    public int port = 8080;
    
    /** 主机名 */
    public String hostname;
    
    public static class ServiceInstance {
        public String appName;
        public String instanceId;
        public String hostName;
        public int port;
        public String ipAddr;
        public InstanceInfo.InstanceStatus status;
    }
    
    @Override
    public void config() {
        instance = this;
        
        serviceUrl = app.conf.getString("eureka", "serviceUrl", serviceUrl);
        appName = app.conf.getString("eureka", "appName", appName);
        port = app.conf.getInt("eureka", "port", port);
        hostname = app.conf.getString("eureka", "hostname", hostname);
        
        if (appName == null) {
            appName = app.conf.getString("app", "name", "litejava-app");
        }
        
        // 设置系统属性（Eureka Client 通过系统属性读取配置）
        System.setProperty("eureka.serviceUrl.default", serviceUrl);
        System.setProperty("eureka.name", appName);
        System.setProperty("eureka.port", String.valueOf(port));
        System.setProperty("eureka.vipAddress", appName);
        System.setProperty("eureka.preferSameZone", "true");
        System.setProperty("eureka.shouldUseDns", "false");
        if (hostname != null) {
            System.setProperty("eureka.hostname", hostname);
        }
        
        // 创建实例配置和信息
        EurekaInstanceConfig instanceConfig = new MyDataCenterInstanceConfig();
        InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
        applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
        
        // 创建 DiscoveryClient（Eureka 1.x API）
        eurekaClient = new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());
        
        // 设置实例状态为 UP
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        
        app.log.info("EurekaPlugin: Registered " + appName + " to " + serviceUrl);
    }
    
    /**
     * 获取服务实例列表
     */
    public List<ServiceInstance> getInstances(String serviceName) {
        List<ServiceInstance> result = new ArrayList<>();
        List<InstanceInfo> instances = eurekaClient.getInstancesByVipAddress(serviceName, false);
        
        for (InstanceInfo info : instances) {
            ServiceInstance si = new ServiceInstance();
            si.appName = info.getAppName();
            si.instanceId = info.getInstanceId();
            si.hostName = info.getHostName();
            si.port = info.getPort();
            si.ipAddr = info.getIPAddr();
            si.status = info.getStatus();
            result.add(si);
        }
        
        return result;
    }
    
    /**
     * 获取下一个可用实例（轮询）
     */
    public ServiceInstance getNextInstance(String serviceName) {
        InstanceInfo info = eurekaClient.getNextServerFromEureka(serviceName, false);
        if (info == null) return null;
        
        ServiceInstance si = new ServiceInstance();
        si.appName = info.getAppName();
        si.instanceId = info.getInstanceId();
        si.hostName = info.getHostName();
        si.port = info.getPort();
        si.ipAddr = info.getIPAddr();
        si.status = info.getStatus();
        return si;
    }
    
    /**
     * 获取所有已注册的应用
     */
    public List<String> getApplications() {
        List<String> apps = new ArrayList<>();
        eurekaClient.getApplications().getRegisteredApplications()
            .forEach(app -> apps.add(app.getName()));
        return apps;
    }
    
    @Override
    public void uninstall() {
        if (applicationInfoManager != null) {
            applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        }
        if (eurekaClient != null) {
            eurekaClient.shutdown();
        }
        instance = null;
    }
}
