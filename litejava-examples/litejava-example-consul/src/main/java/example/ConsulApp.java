package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.discovery.ConsulPlugin;

import java.util.List;
import java.util.Map;

/**
 * Consul 服务发现示例
 * 
 * <h2>前置条件</h2>
 * 启动 Consul: docker run -d -p 8500:8500 consul
 * 
 * <h2>测试</h2>
 * <pre>
 * # 查看服务列表
 * curl http://localhost:8080/services/my-service
 * 
 * # 健康检查
 * curl http://localhost:8080/health
 * </pre>
 */
public class ConsulApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        ConsulPlugin consul = new ConsulPlugin();
        consul.host = "localhost";
        consul.port = 8500;
        consul.serviceName = "my-service";
        consul.servicePort = 8080;
        consul.tags = new String[]{"web", "api"};
        app.use(consul);
        
        // 健康检查端点
        app.get("/health", ctx -> {
            ctx.json(Map.of("status", "UP"));
        });
        
        // 获取服务实例
        app.get("/services/:name", ctx -> {
            String name = ctx.pathParam("name");
            List<String> instances = consul.getHealthyInstances(name);
            ctx.json(Map.of("service", name, "instances", instances));
        });
        
        // 获取一个实例（负载均衡）
        app.get("/services/:name/one", ctx -> {
            String name = ctx.pathParam("name");
            String instance = consul.getOneInstance(name);
            if (instance == null) {
                ctx.status(404).json(Map.of("error", "No healthy instance"));
                return;
            }
            ctx.json(Map.of("instance", instance));
        });
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "Consul Service Discovery Example",
            "service", consul.serviceName,
            "endpoints", Map.of(
                "GET /health", "Health check",
                "GET /services/:name", "Get service instances",
                "GET /services/:name/one", "Get one instance (load balanced)"
            )
        )));
        
        app.run();
    }
}
