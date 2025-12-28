package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.mq.RabbitMQPlugin;

import java.util.Map;

/**
 * RabbitMQ 消息队列示例
 * 
 * <h2>前置条件</h2>
 * 启动 RabbitMQ: docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:management
 * 
 * <h2>测试</h2>
 * <pre>
 * # 发送消息
 * curl -X POST http://localhost:8080/send -H "Content-Type: application/json" -d '{"message":"hello"}'
 * </pre>
 */
public class RabbitMQApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        RabbitMQPlugin rabbit = new RabbitMQPlugin();
        rabbit.host = "localhost";
        rabbit.username = "guest";
        rabbit.password = "guest";
        app.use(rabbit);
        
        // 声明队列
        rabbit.declareQueue("demo-queue");
        
        // 订阅消息
        rabbit.subscribe("demo-queue", message -> {
            System.out.println("Received: " + message);
        });
        
        // 发送消息 API
        app.post("/send", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            String message = (String) body.get("message");
            rabbit.publish("demo-queue", message);
            ctx.json(Map.of("sent", message));
        });
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "RabbitMQ Example",
            "endpoints", Map.of(
                "POST /send", "Send message to RabbitMQ"
            )
        )));
        
        app.run();
    }
}
