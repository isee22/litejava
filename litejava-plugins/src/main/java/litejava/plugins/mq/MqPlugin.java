package litejava.plugins.mq;

import com.rabbitmq.client.*;
import litejava.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * 消息队列插件 - RabbitMQ
 * 
 * 配置示例：
 * rabbitmq:
 *   host: localhost
 *   port: 5672
 *   username: guest
 *   password: guest
 *   virtualHost: /
 * 
 * 使用示例：
 * <pre>{@code
 * MqPlugin mq = app.getPlugin(MqPlugin.class);
 * 
 * // 发送消息
 * mq.publish("order.created", orderJson);
 * 
 * // 订阅消息
 * mq.subscribe("order.created", msg -> {
 *     // 处理消息
 * });
 * }</pre>
 */
public class MqPlugin extends Plugin {
    
    public String host = "localhost";
    public int port = 5672;
    public String username = "guest";
    public String password = "guest";
    public String virtualHost = "/";
    
    private Connection connection;
    private Channel channel;
    private Map<String, String> queueMap = new ConcurrentHashMap<>();
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return channel != null && channel.isOpen();
    }
    
    @Override
    public void config() {
        host = app.conf.getString("rabbitmq", "host", host);
        port = app.conf.getInt("rabbitmq", "port", port);
        username = app.conf.getString("rabbitmq", "username", username);
        password = app.conf.getString("rabbitmq", "password", password);
        virtualHost = app.conf.getString("rabbitmq", "virtualHost", virtualHost);
        
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);
            factory.setAutomaticRecoveryEnabled(true);
            
            connection = factory.newConnection();
            channel = connection.createChannel();
            
            app.log.info("[MQ] RabbitMQ 已连接: " + host + ":" + port);
        } catch (IOException | TimeoutException e) {
            app.log.error("[MQ] 连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 发布消息到 exchange
     */
    public void publish(String exchange, String routingKey, String message) {
        try {
            channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
            channel.basicPublish(exchange, routingKey, 
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            app.log.error("[MQ] 发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 发布消息（简化版，exchange = routingKey）
     */
    public void publish(String topic, String message) {
        publish(topic, topic, message);
    }
    
    /**
     * 发布 JSON 消息
     */
    public void publishJson(String topic, Object data) {
        publish(topic, app.json.stringify(data));
    }
    
    /**
     * 订阅消息
     */
    public void subscribe(String exchange, String routingKey, Consumer<String> handler) {
        try {
            channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
            
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, exchange, routingKey);
            queueMap.put(exchange + ":" + routingKey, queueName);
            
            channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                        AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, StandardCharsets.UTF_8);
                    try {
                        handler.accept(message);
                        channel.basicAck(envelope.getDeliveryTag(), false);
                    } catch (Exception e) {
                        app.log.error("[MQ] 处理消息失败: " + e.getMessage());
                        channel.basicNack(envelope.getDeliveryTag(), false, true);
                    }
                }
            });
            
            app.log.info("[MQ] 订阅: " + exchange + " -> " + routingKey);
        } catch (IOException e) {
            app.log.error("[MQ] 订阅失败: " + e.getMessage());
        }
    }
    
    /**
     * 订阅消息（简化版）
     */
    public void subscribe(String topic, Consumer<String> handler) {
        subscribe(topic, topic, handler);
    }
    
    /**
     * 订阅 JSON 消息
     */
    @SuppressWarnings("unchecked")
    public void subscribeJson(String topic, Consumer<Map<String, Object>> handler) {
        subscribe(topic, msg -> {
            Map<String, Object> data = app.json.parseMap(msg);
            handler.accept(data);
        });
    }
    
    @Override
    public void uninstall() {
        try {
            if (channel != null) channel.close();
            if (connection != null) connection.close();
        } catch (Exception e) {
            app.log.error("[MQ] 关闭失败: " + e.getMessage());
        }
    }
}
