package litejava.plugins.mq;

import litejava.Plugin;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * RabbitMQ 消息队列插件
 * 
 * <h2>配置</h2>
 * <pre>
 * rabbitmq:
 *   host: localhost
 *   port: 5672
 *   username: guest
 *   password: guest
 *   virtualHost: /
 * </pre>
 */
public class RabbitMQPlugin extends Plugin {
    
    public String host = "localhost";
    public int port = 5672;
    public String username = "guest";
    public String password = "guest";
    public String virtualHost = "/";
    
    public Connection connection;
    public Channel channel;
    
    public RabbitMQPlugin() {
    }
    
    /**
     * 构造函数 - 指定主机
     */
    public RabbitMQPlugin(String host) {
        this.host = host;
    }
    
    /**
     * 构造函数 - 指定主机和端口
     */
    public RabbitMQPlugin(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 构造函数 - 指定主机、端口和凭证
     */
    public RabbitMQPlugin(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
    
    @Override
    public void config() {
        // 记录原始值
        String originalHost = host;
        
        // 集中加载配置
        host = app.conf.getString("rabbitmq", "host", host);
        port = app.conf.getInt("rabbitmq", "port", port);
        username = app.conf.getString("rabbitmq", "username", username);
        password = app.conf.getString("rabbitmq", "password", password);
        virtualHost = app.conf.getString("rabbitmq", "virtualHost", virtualHost);
        
        // 如果使用默认值，警告用户
        if (host.equals(originalHost) && app.conf.getString("rabbitmq", "host", null) == null) {
            app.log.warn("RabbitMQPlugin: Using default connection " + host + ":" + port + 
                " (no rabbitmq.host configured)");
        }
        
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);
            
            connection = factory.newConnection();
            channel = connection.createChannel();
            
            app.log.info("RabbitMQPlugin: Connected to " + host + ":" + port);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect RabbitMQ: " + e.getMessage(), e);
        }
    }
    
    public void declareQueue(String queue) {
        declareQueue(queue, true, false, false);
    }
    
    public void declareQueue(String queue, boolean durable, boolean exclusive, boolean autoDelete) {
        try {
            channel.queueDeclare(queue, durable, exclusive, autoDelete, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to declare queue: " + e.getMessage(), e);
        }
    }
    
    public void declareExchange(String exchange, String type) {
        try {
            channel.exchangeDeclare(exchange, type, true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to declare exchange: " + e.getMessage(), e);
        }
    }
    
    public void bindQueue(String queue, String exchange, String routingKey) {
        try {
            channel.queueBind(queue, exchange, routingKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to bind queue: " + e.getMessage(), e);
        }
    }
    
    public void publish(String queue, String message) {
        try {
            channel.basicPublish("", queue, null, message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to publish: " + e.getMessage(), e);
        }
    }
    
    public void publish(String exchange, String routingKey, String message) {
        try {
            channel.basicPublish(exchange, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to publish: " + e.getMessage(), e);
        }
    }
    
    public void subscribe(String queue, Consumer<String> handler) {
        try {
            channel.basicConsume(queue, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                        AMQP.BasicProperties properties, byte[] body) {
                    String message = new String(body, StandardCharsets.UTF_8);
                    handler.accept(message);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to subscribe: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void uninstall() {
        try {
            if (channel != null) channel.close();
            if (connection != null) connection.close();
        } catch (Exception ignored) {}
    }
}
