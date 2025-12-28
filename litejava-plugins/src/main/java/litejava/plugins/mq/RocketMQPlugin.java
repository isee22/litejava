package litejava.plugins.mq;

import litejava.Plugin;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * RocketMQ 消息队列插件
 * 
 * <h2>配置 (application.yml)</h2>
 * <pre>
 * rocketmq:
 *   namesrvAddr: localhost:9876
 *   producerGroup: litejava-producer
 *   consumerGroup: litejava-consumer
 * </pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * app.use(new RocketMQPlugin());
 * 
 * // 发送消息
 * RocketMQPlugin.instance.send("topic", "Hello RocketMQ");
 * RocketMQPlugin.instance.send("topic", "tag", "Hello with tag");
 * 
 * // 订阅消息
 * RocketMQPlugin.instance.subscribe("topic", "*", msg -> {
 *     System.out.println("Received: " + msg.body);
 * });
 * }</pre>
 */
public class RocketMQPlugin extends Plugin {
    
    public static RocketMQPlugin instance;
    
    /** 生产者 */
    public DefaultMQProducer producer;
    
    /** 消费者 Map */
    private Map<String, DefaultMQPushConsumer> consumers = new ConcurrentHashMap<>();
    
    /** NameServer 地址 */
    public String namesrvAddr = "localhost:9876";
    
    /** 生产者组 */
    public String producerGroup = "litejava-producer";
    
    /** 消费者组 */
    public String consumerGroup = "litejava-consumer";
    
    /** 发送超时（毫秒） */
    public int sendTimeout = 3000;
    
    public static class RocketMessage {
        public String topic;
        public String tags;
        public String keys;
        public String body;
        public String msgId;
        public int reconsumeTimes;
        public MessageExt raw;
    }
    
    @Override
    public void config() {
        instance = this;
        
        namesrvAddr = app.conf.getString("rocketmq", "namesrvAddr", namesrvAddr);
        producerGroup = app.conf.getString("rocketmq", "producerGroup", producerGroup);
        consumerGroup = app.conf.getString("rocketmq", "consumerGroup", consumerGroup);
        sendTimeout = app.conf.getInt("rocketmq", "sendTimeout", sendTimeout);
        
        producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(namesrvAddr);
        producer.setSendMsgTimeout(sendTimeout);
        
        try {
            producer.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start RocketMQ producer", e);
        }
        
        app.log.info("RocketMQPlugin: Connected to " + namesrvAddr);
    }
    
    /**
     * 发送消息
     */
    public SendResult send(String topic, String body) {
        return send(topic, "", "", body);
    }
    
    /**
     * 发送消息（带 tag）
     */
    public SendResult send(String topic, String tags, String body) {
        return send(topic, tags, "", body);
    }
    
    /**
     * 发送消息（带 tag 和 keys）
     */
    public SendResult send(String topic, String tags, String keys, String body) {
        try {
            Message msg = new Message(topic, tags, keys, body.getBytes(StandardCharsets.UTF_8));
            return producer.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }
    
    /**
     * 异步发送消息
     */
    public void sendAsync(String topic, String body, Consumer<SendResult> onSuccess, Consumer<Throwable> onError) {
        try {
            Message msg = new Message(topic, body.getBytes(StandardCharsets.UTF_8));
            producer.send(msg, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(SendResult result) {
                    if (onSuccess != null) onSuccess.accept(result);
                }
                @Override
                public void onException(Throwable e) {
                    if (onError != null) onError.accept(e);
                }
            });
        } catch (Exception e) {
            if (onError != null) onError.accept(e);
        }
    }
    
    /**
     * 发送单向消息（不等待响应）
     */
    public void sendOneway(String topic, String body) {
        try {
            Message msg = new Message(topic, body.getBytes(StandardCharsets.UTF_8));
            producer.sendOneway(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send oneway message", e);
        }
    }
    
    /**
     * 订阅消息
     */
    public void subscribe(String topic, String tags, Consumer<RocketMessage> handler) {
        subscribe(topic, tags, consumerGroup, handler);
    }
    
    /**
     * 订阅消息（指定消费者组）
     */
    public void subscribe(String topic, String tags, String group, Consumer<RocketMessage> handler) {
        try {
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
            consumer.setNamesrvAddr(namesrvAddr);
            consumer.subscribe(topic, tags);
            
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                for (MessageExt msg : msgs) {
                    RocketMessage rm = new RocketMessage();
                    rm.topic = msg.getTopic();
                    rm.tags = msg.getTags();
                    rm.keys = msg.getKeys();
                    rm.body = new String(msg.getBody(), StandardCharsets.UTF_8);
                    rm.msgId = msg.getMsgId();
                    rm.reconsumeTimes = msg.getReconsumeTimes();
                    rm.raw = msg;
                    
                    try {
                        handler.accept(rm);
                    } catch (Exception e) {
                        app.log.error("Error processing message: " + e.getMessage());
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            
            consumer.start();
            consumers.put(topic + ":" + group, consumer);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe topic", e);
        }
    }
    
    @Override
    public void uninstall() {
        if (producer != null) {
            producer.shutdown();
        }
        for (DefaultMQPushConsumer consumer : consumers.values()) {
            consumer.shutdown();
        }
        consumers.clear();
        instance = null;
    }
}
