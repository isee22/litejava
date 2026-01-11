package game.hall.service;

import game.hall.model.ServerInfo;
import game.hall.util.HttpUtil;
import game.hall.util.SignUtil;
import litejava.App;
import litejava.plugins.cache.MemoryCachePlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间服务 (对应原作 room_service.js)
 * 
 * 职责:
 * - GameServer 注册/心跳
 * - 服务器状态管理
 * - 负载均衡选择
 * - 房间创建/进入 (调用 GameServer)
 */
public class RoomService {
    
    private static final String KEY_SERVER_INFO = "hall:server:";
    
    private final App app;
    private final MemoryCachePlugin cache;
    private final SignUtil sign;
    private final HttpUtil http;
    
    // GameServer 列表 (对应原作 serverMap)
    private final Map<String, ServerInfo> serverMap = new ConcurrentHashMap<>();
    
    public RoomService(App app, MemoryCachePlugin cache, SignUtil sign, HttpUtil http) {
        this.app = app;
        this.cache = cache;
        this.sign = sign;
        this.http = http;
    }
    
    /**
     * 注册路由 (对应原作 room_service.js 的 app.get)
     */
    public void setupRoutes() {
        // GameServer 注册 (对应原作 /register_gs)
        app.get("/register_gs", ctx -> {
            String clientip = ctx.queryParam("clientip");
            String clientport = ctx.queryParam("clientport");
            String httpPort = ctx.queryParam("httpPort");
            String load = ctx.queryParam("load", "0");
            String gameType = ctx.queryParam("gameType", "default");
            
            ServerInfo info = register(
                clientip,
                Integer.parseInt(clientport),
                Integer.parseInt(httpPort),
                Integer.parseInt(load),
                gameType,
                ctx.clientIP()
            );
            
            if (info == null) {
                ctx.fail(1, "duplicate gsid");
                return;
            }
            ctx.ok(Map.of("ip", info.ip));
        });
        
        // 心跳
        app.get("/heartbeat", ctx -> {
            String id = ctx.queryParam("id");
            int load = Integer.parseInt(ctx.queryParam("load", "0"));
            heartbeat(id, load);
            ctx.ok(null);
        });
    }
    
    /**
     * 注册管理路由
     */
    public void setupAdminRoutes() {
        // 获取所有 GameServer 状态
        app.get("/admin/servers", ctx -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (ServerInfo info : serverMap.values()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", info.id);
                m.put("ip", info.ip);
                m.put("clientip", info.clientip);
                m.put("clientport", info.clientport);
                m.put("httpPort", info.httpPort);
                m.put("load", info.load);
                m.put("gameType", info.gameType);
                m.put("lastHeartbeat", info.lastHeartbeat);
                m.put("online", System.currentTimeMillis() - info.lastHeartbeat < 60000);
                list.add(m);
            }
            ctx.ok(list);
        });
    }
    
    public Map<String, ServerInfo> getServerMap() {
        return serverMap;
    }
    
    // ==================== GameServer 注册 ====================
    
    /**
     * 注册 GameServer
     */
    public ServerInfo register(String clientip, int clientport, int httpPort, int load, String gameType, String realIp) {
        String id = clientip + ":" + clientport;
        
        // realIp 为 null 时使用 clientip (本地开发场景)
        String ip = (realIp != null && !realIp.isEmpty()) ? realIp : clientip;
        
        ServerInfo info = serverMap.get(id);
        if (info != null) {
            // 检查是否重复注册 (不同 IP)
            if (!info.ip.equals(ip) || info.httpPort != httpPort) {
                app.log.warn("duplicate gsid: " + id + ", addr: " + ip + "(" + httpPort + ")");
                return null;
            }
            info.load = load;
            return info;
        }
        
        info = new ServerInfo();
        info.id = id;
        info.ip = ip;
        info.clientip = clientip;
        info.clientport = clientport;
        info.httpPort = httpPort;
        info.load = load;
        info.gameType = gameType;
        info.lastHeartbeat = System.currentTimeMillis();
        serverMap.put(id, info);
        
        app.log.info("game server registered.");
        app.log.info("\tid: " + id);
        app.log.info("\taddr: " + ip);
        app.log.info("\thttp port: " + httpPort);
        app.log.info("\tsocket clientport: " + clientport);
        
        // 延迟同步服务器信息 (GameServer HTTP 服务可能还没启动)
        // syncServerInfo(info);  // 新启动的服务器没有房间需要同步
        
        saveServerInfo(info);
        return info;
    }
    
    /**
     * 同步服务器房间信息 (对应原作 /get_server_info 调用)
     */
    private void syncServerInfo(ServerInfo info) {
        Map<String, Object> reqdata = new HashMap<>();
        reqdata.put("serverid", info.id);
        reqdata.put("sign", sign.sign(info.id));
        
        String url = "http://" + info.ip + ":" + info.httpPort + "/get_server_info";
        String resp = http.get(url, reqdata);
        
        // GameServer 可能还没启动 HTTP 服务，忽略错误
        if (resp == null || resp.isEmpty()) {
            return;
        }
        
        Map<String, Object> data = app.json.parseMap(resp);
        if (data != null && SignUtil.toInt(data.get("errcode")) == 0) {
            // 原作: 同步 userroominfo (userId -> roomId 映射)
        }
    }
    
    // ==================== 负载均衡 ====================
    
    /**
     * 选择服务器 - 负载最低优先
     */
    public ServerInfo chooseServer(String gameType) {
        ServerInfo best = null;
        for (ServerInfo info : serverMap.values()) {
            if (System.currentTimeMillis() - info.lastHeartbeat > 60000) continue;
            if (gameType != null && !gameType.equals(info.gameType) && !"default".equals(info.gameType)) {
                continue;
            }
            if (best == null || info.load < best.load) {
                best = info;
            }
        }
        return best;
    }
    
    // ==================== 房间操作 ====================
    
    /**
     * 创建房间 (调用 GameServer)
     */
    public Map<String, Object> createRoom(long userId, int gems, String conf, ServerInfo server) {
        Map<String, Object> reqdata = new HashMap<>();
        reqdata.put("userid", userId);
        reqdata.put("gems", gems);
        reqdata.put("conf", conf);
        // 签名格式: md5(userId + conf + priKey) - 旧格式
        reqdata.put("sign", sign.sign(userId, conf));
        
        String url = "http://" + server.ip + ":" + server.httpPort + "/create_room";
        String resp = http.get(url, reqdata);
        return app.json.parseMap(resp);
    }
    
    /**
     * 进入房间 (调用 GameServer)
     */
    public Map<String, Object> enterRoom(long userId, String name, String roomId, ServerInfo server) {
        Map<String, Object> reqdata = new HashMap<>();
        reqdata.put("userid", userId);
        reqdata.put("name", name);
        reqdata.put("roomid", roomId);
        reqdata.put("sign", sign.sign(userId, name, roomId));
        
        String url = "http://" + server.ip + ":" + server.httpPort + "/enter_room";
        String resp = http.get(url, reqdata);
        return app.json.parseMap(resp);
    }
    
    /**
     * 检查房间是否运行中
     */
    public boolean isRoomRunning(String roomId, ServerInfo server) {
        Map<String, Object> reqdata = new HashMap<>();
        reqdata.put("roomid", roomId);
        reqdata.put("sign", sign.sign(roomId));
        
        String url = "http://" + server.ip + ":" + server.httpPort + "/is_room_runing";
        String resp = http.get(url, reqdata);
        Map<String, Object> data = app.json.parseMap(resp);
        
        return data != null && SignUtil.toInt(data.get("errcode")) == 0 && Boolean.TRUE.equals(data.get("runing"));
    }
    
    // ==================== 服务器管理 ====================
    
    public void heartbeat(String id, int load) {
        ServerInfo info = serverMap.get(id);
        if (info != null) {
            info.load = load;
            info.lastHeartbeat = System.currentTimeMillis();
            saveServerInfo(info);
        }
    }
    
    public void cleanup() {
        long now = System.currentTimeMillis();
        serverMap.entrySet().removeIf(e -> {
            boolean timeout = now - e.getValue().lastHeartbeat > 60000;
            if (timeout) {
                cache.del(KEY_SERVER_INFO + e.getKey());
                app.log.info("GameServer timeout: " + e.getKey());
            }
            return timeout;
        });
    }
    
    private void saveServerInfo(ServerInfo info) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", info.id);
        m.put("ip", info.ip);
        m.put("clientip", info.clientip);
        m.put("clientport", info.clientport);
        m.put("httpPort", info.httpPort);
        m.put("load", info.load);
        m.put("gameType", info.gameType);
        m.put("lastHeartbeat", info.lastHeartbeat);
        cache.set(KEY_SERVER_INFO + info.id, app.json.stringify(m), 120);
    }
}
