package litejava.plugins.mq;

import litejava.Plugin;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Kafka 消息队列插件
 * 
 * <h2>配置</h2>
 * <pre>
 * kafka:
 *   bootstrapServers: localhost:9092
 *   groupId: my-group
 *   autoCommit: true
 * </pre>
 */
public class KafkaPlugin extends Plugin {
    
    public String bootstrapServers = "localhost:9092";
    public String groupId = "litejava-group";
    public boolean autoCommit = true;
    
    public KafkaProducer<String, String> producer;
    public Map<String, KafkaConsumer<String, String>> consumers = new HashMap<>();
    public ExecutorService executor;
    public volatile boolean running = true;
    
    public KafkaPlugin() {
    }
    
    /**
     * 构造函数 - 指定 Kafka 服务器地址
     */
    public KafkaPlugin(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }
    
    /**
     * 构造函数 - 指定 Kafka 服务器地址和消费者组
     */
    public KafkaPlugin(String bootstrapServers, String groupId) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
    }
    
    @Override
    public void config() {
        // 记录原始值
        String originalServers = bootstrapServers;
        
        // 集中加载配置
        bootstrapServers = app.conf.getString("kafka", "bootstrapServers", bootstrapServers);
        groupId = app.conf.getString("kafka", "groupId", groupId);
        autoCommit = app.conf.getBool("kafka", "autoCommit", autoCommit);
        
        // 如果使用默认值，警告用户
        if (bootstrapServers.equals(originalServers) && 
            app.conf.getString("kafka", "bootstrapServers", null) == null) {
            app.log.warn("KafkaPlugin: Using default bootstrap servers " + bootstrapServers + 
                " (no kafka.bootstrapServers configured)");
        }
        
        // 初始化生产者
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        producer = new KafkaProducer<>(props);
        executor = Executors.newCachedThreadPool();
        
        app.log.info("KafkaPlugin: Connected to " + bootstrapServers);
    }
    
    public Future<RecordMetadata> send(String topic, String message) {
        return producer.send(new ProducerRecord<>(topic, message));
    }
    
    public Future<RecordMetadata> send(String topic, String key, String message) {
        return producer.send(new ProducerRecord<>(topic, key, message));
    }
    
    public void sendAsync(String topic, String message, Callback callback) {
        producer.send(new ProducerRecord<>(topic, message), callback);
    }
    
    public void subscribe(String topic, Consumer<String> handler) {
        subscribe(Collections.singletonList(topic), handler);
    }
    
    public void subscribe(List<String> topics, Consumer<String> handler) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(topics);
        
        String key = String.join(",", topics);
        consumers.put(key, consumer);
        
        executor.submit(() -> {
            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        handler.accept(record.value());
                    }
                } catch (Exception e) {
                    if (running) {
                        app.log.warn("Kafka consumer error: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public void uninstall() {
        running = false;
        if (producer != null) {
            producer.close();
        }
        for (KafkaConsumer<String, String> consumer : consumers.values()) {
            consumer.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
