package litejava.plugins.http;

import litejava.Context;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * SSE (Server-Sent Events) 插件 - 服务端推送
 * 
 * <h2>依赖</h2>
 * 无外部依赖
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * SsePlugin sse = new SsePlugin();
 * 
 * // SSE 端点
 * app.get("/events", ctx -> {
 *     sse.connect(ctx, "user-123", client -> {
 *         client.send("connected", "Welcome!");
 *     });
 * });
 * 
 * // 发送消息给特定客户端
 * sse.send("user-123", "message", "Hello!");
 * 
 * // 广播给所有客户端
 * sse.broadcast("update", "{\"count\": 42}");
 * }</pre>
 */
public class SsePlugin {
    
    private final Map<String, SseClient> clients = new ConcurrentHashMap<>();
    
    public void connect(Context ctx, String clientId, Consumer<SseClient> onConnect) {
        ctx.header("Content-Type", "text/event-stream");
        ctx.header("Cache-Control", "no-cache");
        ctx.header("Connection", "keep-alive");
        
        SseClient client = new SseClient(clientId, ctx);
        clients.put(clientId, client);
        
        if (onConnect != null) {
            onConnect.accept(client);
        }
    }
    
    public void send(String clientId, String event, String data) {
        SseClient client = clients.get(clientId);
        if (client != null) {
            client.send(event, data);
        }
    }
    
    public void broadcast(String event, String data) {
        clients.values().forEach(c -> c.send(event, data));
    }
    
    public void disconnect(String clientId) {
        clients.remove(clientId);
    }
    
    public static class SseClient {
        public final String id;
        private final Context ctx;
        
        SseClient(String id, Context ctx) {
            this.id = id;
            this.ctx = ctx;
        }
        
        public void send(String event, String data) {
            StringBuilder sb = new StringBuilder();
            if (event != null) {
                sb.append("event: ").append(event).append("\n");
            }
            sb.append("data: ").append(data).append("\n\n");
            ctx.text(sb.toString());
        }
        
        public void send(String data) {
            send(null, data);
        }
    }
}
