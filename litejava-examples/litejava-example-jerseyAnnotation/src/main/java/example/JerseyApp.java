package example;

import example.resource.UserResource;
import litejava.App;
import litejava.plugin.HttpServerPlugin;
import litejava.plugins.annotation.JerseyRuntimePlugin;
import litejava.plugins.json.JacksonPlugin;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jersey 完整运行时示例 - 使用 JerseyRuntimePlugin
 * 
 * <p>集成完整 Jersey JAX-RS 运行时，支持所有 JAX-RS 特性：
 * Filter、Interceptor、ExceptionMapper、异步处理等。
 * 
 * <h2>运行</h2>
 * <pre>
 * cd litejava-example-jersey
 * mvn package -DskipTests
 * java -jar target/litejava-example-jersey-1.0.0-jdk8-shaded.jar
 * </pre>
 * 
 * <h2>测试</h2>
 * <pre>
 * curl http://localhost:8080/api/users
 * curl http://localhost:8080/api/users/1
 * </pre>
 */
public class JerseyApp {
    
    public static void main(String[] args) {
        App app = new App();
        app.use(new JacksonPlugin());
        app.use(new HttpServerPlugin());
        
        // Jersey 完整运行时 - 手动注册 Resource 类（shaded jar 中包扫描可能失效）
        JerseyRuntimePlugin jersey = new JerseyRuntimePlugin();
        jersey.basePath = "/api";
        jersey.register(UserResource.class);
        jersey.register(JacksonFeature.class);  // 注册 Jackson JSON 支持
        app.use(jersey);
        
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("message", "LiteJava Jersey Runtime Example");
        info.put("endpoints", "/api/users");
        app.get("/", ctx -> ctx.json(info));
        
        app.run();
    }
}
