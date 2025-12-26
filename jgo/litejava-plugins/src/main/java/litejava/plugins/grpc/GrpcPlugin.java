package litejava.plugins.grpc;

import litejava.Plugin;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * gRPC 插件
 * 
 * <p>提供轻量级 gRPC 服务支持。
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * GrpcPlugin grpc = new GrpcPlugin()
 *     .port(9090)
 *     .service("UserService", service -> {
 *         service.unary("GetUser", req -> {
 *             long id = (Long) req.get("id");
 *             return Map.of("id", id, "name", "John");
 *         });
 *     });
 * 
 * app.use(grpc);
 * }</pre>
 */
public class GrpcPlugin extends Plugin {
    
    public int port = 9090;
    
    private Map<String, ServiceDefinition> services = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    
    public static class ServiceDefinition {
        public String name;
        public Map<String, Function<Map<String, Object>, Object>> methods = new ConcurrentHashMap<>();
        
        public ServiceDefinition unary(String method, Function<Map<String, Object>, Object> handler) {
            methods.put(method, handler);
            return this;
        }
    }
    
    public GrpcPlugin port(int port) {
        this.port = port;
        return this;
    }
    
    public GrpcPlugin service(String name, Consumer<ServiceDefinition> configurer) {
        ServiceDefinition service = new ServiceDefinition();
        service.name = name;
        configurer.accept(service);
        services.put(name, service);
        return this;
    }
    
    @Override
    public void config() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "grpc-worker");
            t.setDaemon(true);
            return t;
        });
        
        Thread serverThread = new Thread(this::startServer, "grpc-server");
        serverThread.setDaemon(true);
        serverThread.start();
        
        // HTTP/JSON 网关
        app.get("/grpc/services", ctx -> {
            Map<String, Object> result = new ConcurrentHashMap<>();
            for (ServiceDefinition service : services.values()) {
                result.put(service.name, service.methods.keySet());
            }
            ctx.json(result);
        });
        
        app.post("/grpc/:service/:method", ctx -> {
            String serviceName = ctx.pathParam("service");
            String methodName = ctx.pathParam("method");
            
            ServiceDefinition service = services.get(serviceName);
            if (service == null) {
                ctx.status(404).json(Map.of("error", "Service not found: " + serviceName));
                return;
            }
            
            Function<Map<String, Object>, Object> handler = service.methods.get(methodName);
            if (handler == null) {
                ctx.status(404).json(Map.of("error", "Method not found: " + methodName));
                return;
            }
            
            try {
                Map<String, Object> request = ctx.bindJSON();
                Object response = handler.apply(request);
                ctx.json(response);
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", e.getMessage()));
            }
        });
    }
    
    @Override
    public void uninstall() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("[gRPC] Server started on port " + port);
            
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    executor.submit(() -> handleClient(client));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[gRPC] Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[gRPC] Failed to start server: " + e.getMessage());
        }
    }
    
    private void handleClient(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter writer = new PrintWriter(client.getOutputStream(), true)) {
            
            String line = reader.readLine();
            if (line == null) return;
            
            String[] parts = line.split("/", 2);
            if (parts.length != 2) {
                writer.println("{\"error\":\"Invalid request format\"}");
                return;
            }
            
            String serviceName = parts[0];
            String methodName = parts[1];
            
            StringBuilder body = new StringBuilder();
            while (reader.ready()) {
                body.append((char) reader.read());
            }
            
            ServiceDefinition service = services.get(serviceName);
            if (service == null) {
                writer.println("{\"error\":\"Service not found\"}");
                return;
            }
            
            Function<Map<String, Object>, Object> handler = service.methods.get(methodName);
            if (handler == null) {
                writer.println("{\"error\":\"Method not found\"}");
                return;
            }
            
            try {
                Map<String, Object> request = parseJson(body.toString());
                Object response = handler.apply(request);
                writer.println(toJson(response));
            } catch (Exception e) {
                writer.println("{\"error\":\"" + e.getMessage() + "\"}");
            }
            
        } catch (IOException e) {
            System.err.println("[gRPC] Client error: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
    
    private Map<String, Object> parseJson(String json) {
        Map<String, Object> map = new ConcurrentHashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return map;
        
        json = json.substring(1, json.length() - 1);
        for (String pair : json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String value = kv[1].trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    map.put(key, value.substring(1, value.length() - 1));
                } else if (value.equals("null")) {
                    map.put(key, null);
                } else {
                    try {
                        map.put(key, Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }
    
    private String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof String) {
            return "\"" + obj + "\"";
        }
        return obj.toString();
    }
}
