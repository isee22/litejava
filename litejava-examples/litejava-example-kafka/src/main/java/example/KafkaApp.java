package example;

import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.mq.KafkaPlugin;

import java.util.Map;

/**
 * Kafka 消息队列示例
 * 
 * <h2>前置条件</h2>
 * 启动 Kafka: docker run -d -p 9092:9092 apache/kafka
 * 
 * <h2>测试</h2>
 * <pre>
 * # 发送消息
 * curl -X POST http://localhost:8080/send -H "Content-Type: application/json" -d '{"message":"hello"}'
 * 
 * # 查看接收的消息（控制台输出）
 * </pre>
 */
public class KafkaApp {
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        
        KafkaPlugin kafka = new KafkaPlugin();
        kafka.bootstrapServers = "localhost:9092";
        kafka.groupId = "litejava-demo";
        app.use(kafka);
        
        // 订阅消息
        kafka.subscribe("demo-topic", message -> {
            System.out.println("Received: " + message);
        });
        
        // 发送消息 API
        app.post("/send", ctx -> {
            Map<String, Object> body = ctx.bindJSON();
            String message = (String) body.get("message");
            kafka.send("demo-topic", message);
            ctx.json(Map.of("sent", message));
        });
        
        app.get("/", ctx -> ctx.json(Map.of(
            "message", "Kafka Example",
            "endpoints", Map.of(
                "POST /send", "Send message to Kafka"
            )
        )));
        
        app.run();
    }
}
