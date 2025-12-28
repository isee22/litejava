package example;

import litejava.App;
import litejava.plugin.HttpServerPlugin;
import litejava.plugins.annotation.JerseyRuntimePlugin;

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
        app.use(new HttpServerPlugin());
        
        // Jersey 完整运行时
        JerseyRuntimePlugin jersey = new JerseyRuntimePlugin();
        jersey.basePath = "/api";
        jersey.packages = "example.resource";
        app.use(jersey);
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "LiteJava Jersey Runtime Example",
            "endpoints", "/api/users"
        )));
        
        app.run(8080);
    }
}
