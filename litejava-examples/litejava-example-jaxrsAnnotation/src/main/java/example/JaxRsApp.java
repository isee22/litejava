package example;

import litejava.App;
import litejava.plugin.HttpServerPlugin;
import litejava.plugins.annotation.JaxRsAnnotationPlugin;
import litejava.plugins.json.JacksonPlugin;

import java.util.Map;

/**
 * JAX-RS 注解路由示例 - 使用 JaxRsAnnotationPlugin
 * 
 * <p>轻量级实现，只解析 JAX-RS 注解并注册到 LiteJava 路由。
 * 
 * <h2>运行</h2>
 * <pre>
 * cd litejava-example-jaxrs
 * mvn package -DskipTests
 * java -jar target/litejava-example-jaxrs-1.0.0-jdk8-shaded.jar
 * </pre>
 * 
 * <h2>测试</h2>
 * <pre>
 * curl http://localhost:8080/api/users
 * curl http://localhost:8080/api/users/1
 * curl -X POST http://localhost:8080/api/users -H "Content-Type: application/json" -d '{"name":"charlie"}'
 * </pre>
 */
public class JaxRsApp {
    
    public static void main(String[] args) {
        App app = new App();
        app.use(new HttpServerPlugin());
        app.use(new JacksonPlugin());
        
        // 零配置！自动扫描 classpath 中的 @Path 注解类
        app.use(new JaxRsAnnotationPlugin());
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "LiteJava JAX-RS Annotation Example",
            "endpoints", "/api/users"
        )));
        
        app.run();
    }
}
