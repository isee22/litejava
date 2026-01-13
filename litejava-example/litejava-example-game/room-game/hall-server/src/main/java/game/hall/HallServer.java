package game.hall;

import game.hall.model.ServerInfo;
import game.hall.util.SignUtil;
import game.hall.vo.MatchUserVO;
import game.hall.vo.RoomInfoVO;
import game.hall.vo.RoomResultVO;
import litejava.App;
import litejava.plugins.LiteJava;
import litejava.plugins.cache.RedisCachePlugin;
import litejava.plugins.http.RecoveryPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 大厅服务器
 */
public class HallServer {
    
    public void start(App app) {
        int port = app.conf.getInt("server", "httpPort", 8201);
        String roomPriKey = app.conf.getString("server", "roomPriKey", "ROOM_PRI_KEY_2024");
        
        // 全局异常处理
        app.use(new RecoveryPlugin());
        
        // Redis 缓存
        RedisCachePlugin cache = new RedisCachePlugin();
        app.use(cache);
        
        // 初始化全局单例
        SignUtil.init(roomPriKey);
        G.init(app, cache);
        
        // 注册路由
        setupRoutes(app);
        
        // 定时任务
        startScheduler();
        
        app.log.info("=== Hall Server ===");
        app.log.info("HTTP: http://localhost:" + port);
        app.run(port);
    }

    private void setupRoutes(App app) {
        // ==================== GameServer 内部接口 ====================
        
        app.get("/register_gs", ctx -> {
            String clientip = ctx.queryParam("clientip");
            int clientport = Integer.parseInt(ctx.queryParam("clientport"));
            int httpPort = Integer.parseInt(ctx.queryParam("httpPort"));
            int load = Integer.parseInt(ctx.queryParam("load", "0"));
            String gameType = ctx.queryParam("gameType", "default");
            
            ServerInfo info = G.registry.register(clientip, clientport, httpPort, load, gameType, ctx.clientIP());
            if (info == null) {
                ctx.fail(1, "register failed");
                return;
            }
            ctx.ok(Map.of("ip", info.ip));
        });
        
        app.get("/heartbeat", ctx -> {
            String id = ctx.queryParam("id");
            int load = Integer.parseInt(ctx.queryParam("load", "0"));
            G.registry.heartbeat(id, load);
            ctx.ok(null);
        });
        
        app.get("/server_restart", ctx -> {
            String serverId = ctx.queryParam("serverId");
            int cleared = G.registry.onServerRestart(serverId);
            ctx.ok(Map.of("cleared", cleared));
        });
        
        app.get("/room_destroyed", ctx -> {
            String roomId = ctx.queryParam("roomId");
            G.roomService.onRoomDestroyed(roomId);
            ctx.ok(null);
        });
        
        app.get("/clear_user_room", ctx -> {
            long userId = Long.parseLong(ctx.queryParam("userId"));
            G.roomService.onUserExit(userId);
            ctx.ok(null);
        });
        
        app.get("/update_room_state", ctx -> {
            String roomId = ctx.queryParam("roomId");
            int playerCount = Integer.parseInt(ctx.queryParam("playerCount", "0"));
            boolean joinable = "true".equals(ctx.queryParam("joinable", "true"));
            G.roomService.updateRoomState(roomId, playerCount, joinable);
            ctx.ok(null);
        });
        
        // ==================== 客户端接口 - 房间 ====================
        
        app.post("/quick_start", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            String gameType = (String) req.getOrDefault("gameType", "doudizhu");
            int roomLevel = toInt(req.getOrDefault("roomLevel", 0));
            
            RoomResultVO result = G.roomService.quickStart(userId, gameType, roomLevel);
            ctx.ok(result);
        });
        
        app.post("/create_room", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            String gameType = (String) req.getOrDefault("gameType", "doudizhu");
            
            RoomResultVO result = G.roomService.createRoom(userId, gameType);
            ctx.ok(result);
        });
        
        app.post("/enter_room", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            String roomId = req.get("roomId") != null ? String.valueOf(req.get("roomId")) : null;
            
            RoomResultVO result = G.roomService.joinRoom(userId, roomId);
            ctx.ok(result);
        });
        
        app.post("/clear_user_room", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            G.roomService.onUserExit(userId);
            ctx.ok(null);
        });
        
        app.post("/get_user_room", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            ctx.ok(G.roomService.getUserRoom(userId));
        });
        
        app.post("/reconnect", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            RoomResultVO result = G.roomService.reconnect(userId);
            ctx.ok(result);
        });
        
        app.post("/room_list", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            String gameType = (String) req.getOrDefault("gameType", "doudizhu");
            int limit = toInt(req.getOrDefault("limit", 20));
            List<RoomInfoVO> rooms = G.roomService.listJoinableRooms(gameType, limit);
            ctx.ok(rooms);
        });

        // ==================== 客户端接口 - 匹配 ====================
        
        app.post("/match/join", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            String name = (String) req.getOrDefault("name", "玩家" + userId);
            String gameType = (String) req.getOrDefault("gameType", "moba");
            String conf = req.get("conf") != null ? app.json.stringify(req.get("conf")) : "{}";
            int rank = toInt(req.getOrDefault("rank", 1000));
            
            G.matchService.joinQueue(userId, name, gameType, conf, rank);
            ctx.ok(null);
        });
        
        app.post("/match/cancel", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            String gameType = (String) req.getOrDefault("gameType", "moba");
            
            G.matchService.cancelMatch(userId, gameType);
            ctx.ok(null);
        });
        
        app.post("/match/status", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = toLong(req.get("userId"));
            
            MatchUserVO status = G.matchService.getMatchStatus(userId);
            ctx.ok(status);
        });
        
        // ==================== 管理接口 ====================
        
        app.get("/admin/servers", ctx -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (ServerInfo info : G.registry.getAll()) {
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
        
        app.get("/health", ctx -> ctx.ok(null));
    }
    
    private void startScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        scheduler.scheduleAtFixedRate(G.registry::cleanup, 30, 30, TimeUnit.SECONDS);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                G.matchService.doMatch("moba");
                G.matchService.doMatch("werewolf");
            } catch (Exception e) {
                G.app.log.error("匹配执行失败: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                G.matchService.cleanupExpired("moba");
                G.matchService.cleanupExpired("werewolf");
            } catch (Exception e) {
                G.app.log.error("匹配清理失败: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    private long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String) return Long.parseLong((String) obj);
        return 0;
    }
    
    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) return Integer.parseInt((String) obj);
        return 0;
    }
    
    public static void main(String[] args) {
        App app = LiteJava.create();
        new HallServer().start(app);
    }
}
