package litejava.plugins.config;

import litejava.Plugin;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Nacos 配置中心插件 - 支持动态配置更新
 * 
 * <h2>配置</h2>
 * <pre>
 * nacos:
 *   serverAddr: localhost:8848
 *   namespace: 
 *   group: DEFAULT_GROUP
 *   dataId: application
 *   username:
 *   password:
 * </pre>
 */
public class NacosConfigPlugin extends Plugin {
    
    public String serverAddr = "localhost:8848";
    public String namespace = "";
    public String group = "DEFAULT_GROUP";
    public String dataId = "application";
    public String username;
    public String password;
    
    public ConfigService configService;
    public String currentConfig;
    
    public NacosConfigPlugin() {
    }
    
    /**
     * 构造函数 - 指定服务器地址
     */
    public NacosConfigPlugin(String serverAddr) {
        this.serverAddr = serverAddr;
    }
    
    /**
     * 构造函数 - 指定服务器地址和 dataId
     */
    public NacosConfigPlugin(String serverAddr, String dataId) {
        this.serverAddr = serverAddr;
        this.dataId = dataId;
    }
    
    /**
     * 构造函数 - 指定服务器地址、dataId 和 group
     */
    public NacosConfigPlugin(String serverAddr, String dataId, String group) {
        this.serverAddr = serverAddr;
        this.dataId = dataId;
        this.group = group;
    }
    
    /**
     * 构造函数 - 指定服务器地址和凭证
     */
    public NacosConfigPlugin(String serverAddr, String username, String password, String dataId) {
        this.serverAddr = serverAddr;
        this.username = username;
        this.password = password;
        this.dataId = dataId;
    }
    
    @Override
    public void config() {
        // 集中加载配置
        serverAddr = app.conf.getString("nacos", "serverAddr", serverAddr);
        namespace = app.conf.getString("nacos", "namespace", namespace);
        group = app.conf.getString("nacos", "group", group);
        dataId = app.conf.getString("nacos", "dataId", dataId);
        username = app.conf.getString("nacos", "username", username);
        password = app.conf.getString("nacos", "password", password);
        
        try {
            Properties props = new Properties();
            props.put("serverAddr", serverAddr);
            if (namespace != null && !namespace.isEmpty()) {
                props.put("namespace", namespace);
            }
            if (username != null && !username.isEmpty()) {
                props.put("username", username);
                props.put("password", password);
            }
            
            configService = NacosFactory.createConfigService(props);
            currentConfig = configService.getConfig(dataId, group, 5000);
            
            app.log.info("NacosConfigPlugin: Connected to " + serverAddr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect Nacos: " + e.getMessage(), e);
        }
    }
    
    public String getConfig() {
        return currentConfig;
    }
    
    public String getConfig(String dataId, String group) {
        try {
            return configService.getConfig(dataId, group, 5000);
        } catch (Exception e) {
            app.log.warn("Failed to get config: " + dataId);
            return null;
        }
    }
    
    public void addListener(Consumer<String> callback) {
        addListener(dataId, group, callback);
    }
    
    public void addListener(String dataId, String group, Consumer<String> callback) {
        try {
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }
                
                @Override
                public void receiveConfigInfo(String configInfo) {
                    currentConfig = configInfo;
                    callback.accept(configInfo);
                }
            });
        } catch (Exception e) {
            app.log.warn("Failed to add listener: " + e.getMessage());
        }
    }
    
    public boolean publishConfig(String content) {
        return publishConfig(dataId, group, content);
    }
    
    public boolean publishConfig(String dataId, String group, String content) {
        try {
            return configService.publishConfig(dataId, group, content);
        } catch (Exception e) {
            app.log.warn("Failed to publish config: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void uninstall() {
        if (configService != null) {
            try {
                configService.shutDown();
            } catch (Exception ignored) {}
        }
    }
}
