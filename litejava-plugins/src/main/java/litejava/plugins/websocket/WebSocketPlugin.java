package litejava.plugins.websocket;

import litejava.Plugin;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * WebSocket 插件 - 实时双向通信
 * 
 * <h2>配置</h2>
 * <pre>{@code
 * websocket.port=8081
 * websocket.path=/ws
 * websocket.maxFrameSize=65536
 * websocket.pingInterval=30000
 * }</pre>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 基础用法
 * WebSocketPlugin ws = new WebSocketPlugin(8081);
 * ws.onConnect = session -> {
 *     System.out.println("Connected: " + session.id);
 *     session.send("Welcome!");
 * };
 * ws.onMessage = (session, msg) -> {
 *     session.send("Echo: " + msg);
 * };
 * ws.onClose = session -> {
 *     System.out.println("Disconnected: " + session.id);
 * };
 * app.use(ws);
 * 
 * // 房间/频道
 * ws.onMessage = (session, msg) -> {
 *     if (msg.startsWith("join:")) {
 *         String room = msg.substring(5);
 *         ws.join(session, room);
 *     } else {
 *         String room = (String) session.data.get("room");
 *         ws.broadcastToRoom(room, msg);
 *     }
 * };
 * 
 * // 二进制消息
 * ws.onBinary = (session, data) -> {
 *     session.sendBinary(data);
 * };
 * }</pre>
 */
public class WebSocketPlugin extends Plugin {
    
    public int port;
    public String path = "/ws";
    public int maxFrameSize = 65536;
    public int pingInterval = 30000;  // ms, 0 = disabled
    
    public ServerSocket serverSocket;
    public final Map<String, WsSession> sessions = new ConcurrentHashMap<>();
    public final Map<String, Set<WsSession>> rooms = new ConcurrentHashMap<>();
    public ExecutorService executor;
    public ScheduledExecutorService pingScheduler;
    
    // 事件回调
    public Consumer<WsSession> onConnect;
    public BiConsumer<WsSession, String> onMessage;
    public BiConsumer<WsSession, byte[]> onBinary;
    public Consumer<WsSession> onClose;
    public BiConsumer<WsSession, Throwable> onError;
    
    public WebSocketPlugin() {}
    
    public WebSocketPlugin(int port) {
        this.port = port;
    }
    
    public WebSocketPlugin(int port, String path) {
        this.port = port;
        this.path = path;
    }
    
    @Override
    public void config() {
        port = app.conf.getInt("websocket", "port", port);
        path = app.conf.getString("websocket", "path", path);
        maxFrameSize = app.conf.getInt("websocket", "maxFrameSize", maxFrameSize);
        pingInterval = app.conf.getInt("websocket", "pingInterval", pingInterval);
        
        executor = Executors.newCachedThreadPool();
        executor.submit(this::startServer);
        
        // 心跳检测
        if (pingInterval > 0) {
            pingScheduler = Executors.newSingleThreadScheduledExecutor();
            pingScheduler.scheduleAtFixedRate(this::pingAll, pingInterval, pingInterval, TimeUnit.MILLISECONDS);
        }
        
        app.log.info("WebSocket server started on port " + port + " path " + path);
    }
    
    @Override
    public void uninstall() {
        try {
            if (serverSocket != null) serverSocket.close();
            if (executor != null) executor.shutdown();
            if (pingScheduler != null) pingScheduler.shutdown();
            sessions.values().forEach(WsSession::close);
        } catch (IOException e) {
            // ignore
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                executor.submit(() -> handleClient(client));
            }
        } catch (IOException e) {
            // Server closed
        }
    }
    
    private void handleClient(Socket client) {
        WsSession session = null;
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            
            // 解析握手请求
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            String key = null;
            String requestPath = null;
            Map<String, String> headers = new HashMap<>();
            
            // 读取请求行
            String requestLine = reader.readLine();
            if (requestLine != null && requestLine.startsWith("GET ")) {
                String[] parts = requestLine.split(" ");
                if (parts.length >= 2) {
                    requestPath = parts[1].split("\\?")[0];
                }
            }
            
            // 读取请求头
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    String name = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    headers.put(name, value);
                    if ("Sec-WebSocket-Key".equalsIgnoreCase(name)) {
                        key = value;
                    }
                }
            }
            
            // 验证路径
            if (!path.equals(requestPath)) {
                sendHttpError(out, 404, "Not Found");
                client.close();
                return;
            }
            
            // 验证 WebSocket 握手
            if (key == null) {
                sendHttpError(out, 400, "Bad Request");
                client.close();
                return;
            }
            
            // 计算 Accept Key
            String accept = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-1").digest(
                    (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()
                )
            );
            
            // 发送握手响应
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
            out.write(response.getBytes());
            out.flush();
            
            // 创建会话
            session = new WsSession(UUID.randomUUID().toString(), client, in, out, this);
            session.headers.putAll(headers);
            sessions.put(session.id, session);
            
            if (onConnect != null) {
                try {
                    onConnect.accept(session);
                } catch (Exception e) {
                    handleError(session, e);
                }
            }
            
            // 消息循环
            while (!client.isClosed() && session.isOpen) {
                WsFrame frame = readFrame(in);
                if (frame == null) break;
                
                switch (frame.opcode) {
                    case WsFrame.OPCODE_TEXT:
                        if (onMessage != null) {
                            try {
                                onMessage.accept(session, new String(frame.payload, StandardCharsets.UTF_8));
                            } catch (Exception e) {
                                handleError(session, e);
                            }
                        }
                        break;
                    case WsFrame.OPCODE_BINARY:
                        if (onBinary != null) {
                            try {
                                onBinary.accept(session, frame.payload);
                            } catch (Exception e) {
                                handleError(session, e);
                            }
                        }
                        break;
                    case WsFrame.OPCODE_CLOSE:
                        session.isOpen = false;
                        break;
                    case WsFrame.OPCODE_PING:
                        session.sendPong(frame.payload);
                        break;
                    case WsFrame.OPCODE_PONG:
                        session.lastPong = System.currentTimeMillis();
                        break;
                }
            }
            
        } catch (Exception e) {
            if (session != null) {
                handleError(session, e);
            }
        } finally {
            if (session != null) {
                closeSession(session);
            }
        }
    }
    
    private void sendHttpError(OutputStream out, int code, String message) throws IOException {
        String response = "HTTP/1.1 " + code + " " + message + "\r\n" +
            "Content-Length: 0\r\n\r\n";
        out.write(response.getBytes());
        out.flush();
    }
    
    private WsFrame readFrame(InputStream in) throws IOException {
        int b1 = in.read();
        if (b1 == -1) return null;
        
        boolean fin = (b1 & 0x80) != 0;
        int opcode = b1 & 0x0F;
        
        int b2 = in.read();
        if (b2 == -1) return null;
        
        boolean masked = (b2 & 0x80) != 0;
        long len = b2 & 0x7F;
        
        if (len == 126) {
            len = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (len == 127) {
            len = 0;
            for (int i = 0; i < 8; i++) {
                len = (len << 8) | (in.read() & 0xFF);
            }
        }
        
        if (len > maxFrameSize) {
            throw new IOException("Frame too large: " + len);
        }
        
        byte[] mask = new byte[4];
        if (masked) {
            in.read(mask);
        }
        
        byte[] payload = new byte[(int) len];
        int read = 0;
        while (read < len) {
            int r = in.read(payload, read, (int) len - read);
            if (r == -1) return null;
            read += r;
        }
        
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i % 4];
            }
        }
        
        return new WsFrame(fin, opcode, payload);
    }
    
    private void handleError(WsSession session, Throwable e) {
        if (onError != null) {
            try {
                onError.accept(session, e);
            } catch (Exception ignored) {}
        }
    }
    
    private void closeSession(WsSession session) {
        sessions.remove(session.id);
        
        // 从所有房间移除
        for (Set<WsSession> room : rooms.values()) {
            room.remove(session);
        }
        
        if (onClose != null) {
            try {
                onClose.accept(session);
            } catch (Exception ignored) {}
        }
        
        session.close();
    }
    
    private void pingAll() {
        long now = System.currentTimeMillis();
        for (WsSession session : sessions.values()) {
            // 超时检测
            if (session.lastPong > 0 && now - session.lastPong > pingInterval * 2) {
                closeSession(session);
                continue;
            }
            session.sendPing();
        }
    }

    // ==================== 公共方法 ====================
    
    /**
     * 广播消息给所有连接
     */
    public void broadcast(String message) {
        for (WsSession session : sessions.values()) {
            session.send(message);
        }
    }
    
    /**
     * 广播二进制消息给所有连接
     */
    public void broadcastBinary(byte[] data) {
        for (WsSession session : sessions.values()) {
            session.sendBinary(data);
        }
    }
    
    /**
     * 广播消息给指定房间
     */
    public void broadcastToRoom(String room, String message) {
        Set<WsSession> roomSessions = rooms.get(room);
        if (roomSessions != null) {
            for (WsSession session : roomSessions) {
                session.send(message);
            }
        }
    }
    
    /**
     * 广播二进制消息给指定房间
     */
    public void broadcastBinaryToRoom(String room, byte[] data) {
        Set<WsSession> roomSessions = rooms.get(room);
        if (roomSessions != null) {
            for (WsSession session : roomSessions) {
                session.sendBinary(data);
            }
        }
    }
    
    /**
     * 加入房间
     */
    public void join(WsSession session, String room) {
        rooms.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet()).add(session);
    }
    
    /**
     * 离开房间
     */
    public void leave(WsSession session, String room) {
        Set<WsSession> roomSessions = rooms.get(room);
        if (roomSessions != null) {
            roomSessions.remove(session);
        }
    }
    
    /**
     * 获取会话
     */
    public WsSession getSession(String id) {
        return sessions.get(id);
    }
    
    /**
     * 获取所有会话
     */
    public Collection<WsSession> getSessions() {
        return sessions.values();
    }
    
    /**
     * 获取房间内的会话
     */
    public Set<WsSession> getRoomSessions(String room) {
        return rooms.getOrDefault(room, Collections.emptySet());
    }
    
    /**
     * 获取连接数
     */
    public int getSessionCount() {
        return sessions.size();
    }
    
    /**
     * 获取房间连接数
     */
    public int getRoomSessionCount(String room) {
        Set<WsSession> roomSessions = rooms.get(room);
        return roomSessions != null ? roomSessions.size() : 0;
    }

    // ==================== 内部类 ====================
    
    /**
     * WebSocket 会话
     */
    public static class WsSession {
        public final String id;
        public final Socket socket;
        public final InputStream in;
        public final OutputStream out;
        public final WebSocketPlugin plugin;
        public final Map<String, String> headers = new HashMap<>();
        public final Map<String, Object> data = new ConcurrentHashMap<>();
        public volatile boolean isOpen = true;
        public volatile long lastPong = System.currentTimeMillis();
        
        public WsSession(String id, Socket socket, InputStream in, OutputStream out, WebSocketPlugin plugin) {
            this.id = id;
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.plugin = plugin;
        }
        
        /**
         * 发送文本消息
         */
        public void send(String message) {
            if (!isOpen) return;
            try {
                writeFrame(WsFrame.OPCODE_TEXT, message.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                close();
            }
        }
        
        /**
         * 发送二进制消息
         */
        public void sendBinary(byte[] data) {
            if (!isOpen) return;
            try {
                writeFrame(WsFrame.OPCODE_BINARY, data);
            } catch (IOException e) {
                close();
            }
        }
        
        /**
         * 发送 Ping
         */
        public void sendPing() {
            if (!isOpen) return;
            try {
                writeFrame(WsFrame.OPCODE_PING, new byte[0]);
            } catch (IOException e) {
                close();
            }
        }
        
        /**
         * 发送 Pong
         */
        public void sendPong(byte[] payload) {
            if (!isOpen) return;
            try {
                writeFrame(WsFrame.OPCODE_PONG, payload);
            } catch (IOException e) {
                close();
            }
        }
        
        /**
         * 关闭连接
         */
        public void close() {
            if (!isOpen) return;
            isOpen = false;
            try {
                writeFrame(WsFrame.OPCODE_CLOSE, new byte[0]);
                socket.close();
            } catch (IOException ignored) {}
        }
        
        /**
         * 加入房间
         */
        public void joinRoom(String room) {
            plugin.join(this, room);
        }
        
        /**
         * 离开房间
         */
        public void leaveRoom(String room) {
            plugin.leave(this, room);
        }
        
        private synchronized void writeFrame(int opcode, byte[] payload) throws IOException {
            int len = payload.length;
            
            // FIN + opcode
            out.write(0x80 | opcode);
            
            // 长度 (服务端发送不需要 mask)
            if (len < 126) {
                out.write(len);
            } else if (len < 65536) {
                out.write(126);
                out.write((len >> 8) & 0xFF);
                out.write(len & 0xFF);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) {
                    out.write((len >> (i * 8)) & 0xFF);
                }
            }
            
            out.write(payload);
            out.flush();
        }
    }
    
    /**
     * WebSocket 帧
     */
    public static class WsFrame {
        public static final int OPCODE_CONTINUATION = 0x0;
        public static final int OPCODE_TEXT = 0x1;
        public static final int OPCODE_BINARY = 0x2;
        public static final int OPCODE_CLOSE = 0x8;
        public static final int OPCODE_PING = 0x9;
        public static final int OPCODE_PONG = 0xA;
        
        public final boolean fin;
        public final int opcode;
        public final byte[] payload;
        
        public WsFrame(boolean fin, int opcode, byte[] payload) {
            this.fin = fin;
            this.opcode = opcode;
            this.payload = payload;
        }
    }
}
