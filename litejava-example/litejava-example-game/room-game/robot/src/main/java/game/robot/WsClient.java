package game.robot;

import litejava.App;
import okhttp3.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 客户端 - 基于 OkHttp3
 */
public class WsClient {
    
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build();
    
    @FunctionalInterface
    public interface MessageHandler {
        void handle(int cmd, int code, Map<String, Object> data);
    }
    
    private final App app;
    private final String url;
    private final MessageHandler handler;
    private Runnable onClose;
    private Runnable onOpen;
    
    private WebSocket ws;
    private volatile boolean connected;
    
    public WsClient(String url, MessageHandler handler, App app) {
        this.app = app;
        this.url = url;
        this.handler = handler;
    }
    
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
    
    public void setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen;
    }
    
    @SuppressWarnings("unchecked")
    public void connect() {
        app.log.info("[WsClient] 连接: " + url);
        Request request = new Request.Builder().url(url).build();
        ws = HTTP.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected = true;
                app.log.info("[WsClient] 连接成功: " + url);
                if (onOpen != null) onOpen.run();
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    app.log.info("[WsClient] 收到消息: " + text);
                    Map<String, Object> msg = app.json.parseMap(text);
                    int cmd = ((Number) msg.get("cmd")).intValue();
                    int code = msg.containsKey("code") ? ((Number) msg.get("code")).intValue() : 0;
                    Map<String, Object> data = (Map<String, Object>) msg.get("data");
                    handler.handle(cmd, code, data);
                } catch (Exception e) {
                    app.log.error("[WsClient] 异常: " + e.getMessage());
                }
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                connected = false;
                if (onClose != null) onClose.run();
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                app.log.error("[WsClient] 连接失败: " + t.getMessage());
                connected = false;
                if (onClose != null) onClose.run();
            }
        });
    }
    
    public void send(int cmd, Map<String, Object> data) {
        if (ws == null || !connected) return;
        
        Map<String, Object> msg = new HashMap<>();
        msg.put("cmd", cmd);
        if (data != null) msg.put("data", data);
        ws.send(app.json.stringify(msg));
    }
    
    public void close() {
        if (ws != null) ws.close(1000, null);
        connected = false;
    }
    
    public boolean isConnected() {
        return connected;
    }
}
