package example;

import litejava.App;
import litejava.plugin.HttpServerPlugin;
import litejava.plugins.annotation.SpringMvcAnnotationPlugin;
import litejava.plugins.json.JacksonPlugin;

import java.util.Map;

/**
 * Spring MVC 注解示例 - 使用 SpringMvcAnnotationPlugin
 * 
 * <p>使用 Spring MVC 注解，但不依赖 Spring 容器。
 * 适合熟悉 Spring 注解风格的开发者。
 * 
 * <h2>运行</h2>
 * <pre>
 * cd litejava-example-springmvc
 * mvn package -DskipTests
 * java -jar target/litejava-example-springmvc-1.0.0-jdk8-shaded.jar
 * </pre>
 * 
 * <h2>测试</h2>
 * <pre>
 * curl http://localhost:8080/api/users
 * curl http://localhost:8080/api/users/1
 * </pre>
 */
public class SpringMvcApp {
    
    public static void main(String[] args) {
        App app = new App();
        app.use(new HttpServerPlugin());
        app.use(new JacksonPlugin());
        
        // 扫描 example 包下的 @RestController/@Controller
        SpringMvcAnnotationPlugin springMvc = new SpringMvcAnnotationPlugin();
        springMvc.packages = "example";
        app.use(springMvc);
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "LiteJava Spring MVC Annotation Example",
            "endpoints", "/api/users"
        )));
        
        app.run();
    }
}
