package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.config.NacosConfigPlugin;

import java.util.Map;

/**
 * Nacos 配置中心示例
 * 
 * <h2>前置条件</h2>
 * 启动 Nacos: docker run -d -p 8848:8848 -e MODE=standalone nacos/nacos-server
 * 
 * <h2>测试</h2>
 * <pre>
 * # 获取配置
 * curl http://localhost:8080/config
 * 
 * # 发布配置
 * curl -X POST http://localhost:8080/config -H "Content-Type: application/json" \
 *   -d '{"content":"server.port=8080\napp.name=demo"}'
 * </pre>
 */
public class NacosApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        NacosConfigPlugin nacos = new NacosConfigPlugin();
        nacos.serverAddr = "localhost:8848";
        nacos.dataId = "demo-config";
        nacos.group = "DEFAULT_GROUP";
        app.use(nacos);
        
        // 监听配置变化
        nacos.addListener(config -> {
            System.out.println("Config updated: " + config);
        });
        
        // 获取配置
        app.get("/config", ctx -> {
            ctx.json(Map.of("config", nacos.getConfig()));
        });
        
        // 发布配置
        app.post("/config", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            String content = (String) body.get("content");
            boolean success = nacos.publishConfig(content);
            ctx.json(Map.of("success", success));
        });
        
        // 获取指定配置
        app.get("/config/:dataId", ctx -> {
            String dataId = ctx.pathParam("dataId");
            String group = ctx.queryParam("group");
            String config = nacos.getConfig(dataId, group != null ? group : "DEFAULT_GROUP");
            ctx.json(Map.of("config", config));
        });
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "Nacos Config Example",
            "endpoints", Map.of(
                "GET /config", "Get current config",
                "POST /config", "Publish config",
                "GET /config/:dataId", "Get specific config"
            )
        )));
        
        app.run();
    }
}
