package game.hall.service;

import game.hall.model.ServerInfo;
import game.hall.util.SignUtil;
import litejava.App;
import litejava.Context;
import litejava.plugins.cache.MemoryCachePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端服务 (对应原作 client_service.js)
 * 
 * 职责:
 * - 创建私人房间 /create_private_room
 * - 加入私人房间 /enter_private_room
 * - 用户房间状态管理
 * - 签名验证
 */
public class ClientService {
    
    private static final String KEY_ROOM_SERVER = "hall:room:";
    private static final String KEY_USER_ROOM = "hall:user:";
    
    private final App app;
    private final MemoryCachePlugin cache;
    private final SignUtil sign;
    private final RoomService roomService;
    private final String accountPriKey;
    
    // 房间号计数器
    private final AtomicLong roomIdCounter = new AtomicLong(System.currentTimeMillis() % 900000);
    
    public ClientService(App app, MemoryCachePlugin cache, SignUtil sign, RoomService roomService, String accountPriKey) {
        this.app = app;
        this.cache = cache;
        this.sign = sign;
        this.roomService = roomService;
        this.accountPriKey = accountPriKey;
    }
    
    /**
     * 注册路由 (对应原作 client_service.js 的 app.get)
     */
    public void setupRoutes() {
        // 创建私人房间 (对应原作 /create_private_room)
        app.get("/create_private_room", ctx -> {
            if (!checkAccount(ctx)) return;
            
            long userId = Long.parseLong(ctx.queryParam("userId"));
            String name = ctx.queryParam("name", "玩家" + userId);
            String conf = ctx.queryParam("conf", "{}");
            String gameType = ctx.queryParam("gameType", "mahjong");
            
            Map<String, Object> result = createPrivateRoom(userId, name, conf, gameType);
            sendResult(ctx, result);
        });
        
        // 加入私人房间 (对应原作 /enter_private_room)
        app.get("/enter_private_room", ctx -> {
            if (!checkAccount(ctx)) return;
            
            long userId = Long.parseLong(ctx.queryParam("userId"));
            String roomId = ctx.queryParam("roomid");
            String name = ctx.queryParam("name", "玩家" + userId);
            
            Map<String, Object> result = enterPrivateRoom(userId, name, roomId);
            sendResult(ctx, result);
        });
        
        // 获取用户房间状态
        app.get("/get_user_room", ctx -> {
            if (!checkAccount(ctx)) return;
            
            long userId = Long.parseLong(ctx.queryParam("userId"));
            Map<String, Object> result = getUserRoom(userId);
            ctx.ok(result);
        });
        
        // 清除用户房间状态
        app.get("/clear_user_room", ctx -> {
            if (!checkAccount(ctx)) return;
            
            long userId = Long.parseLong(ctx.queryParam("userId"));
            clearUserRoom(userId);
            ctx.ok(null);
        });
        
        // 房间销毁通知 (GameServer 调用)
        app.get("/room_destroyed", ctx -> {
            String roomId = ctx.queryParam("roomId");
            onRoomDestroyed(roomId);
            ctx.ok(null);
        });
        
        // 健康检查
        app.get("/health", ctx -> {
            ctx.ok(Map.of("status", "UP", "servers", roomService.getServerMap().size()));
        });
    }
    
    /**
     * 注册管理路由
     */
    public void setupAdminRoutes() {
        // 强制清理房间
        app.post("/admin/clear_room", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            String roomId = (String) req.get("roomId");
            clearRoom(roomId);
            ctx.ok(null);
        });
    }
    
    // ==================== 签名验证 (对应原作 check_account) ====================
    
    /**
     * 验证客户端签名
     */
    private boolean checkAccount(Context ctx) {
        String account = ctx.queryParam("account");
        String clientSign = ctx.queryParam("sign");
        
        if (account == null || clientSign == null) {
            ctx.fail(1, "unknown error");
            return false;
        }
        
        // 验证签名: md5(account + ip + ACCOUNT_PRI_KEY)
        String serverSign = sign.md5(account + ctx.clientIP() + accountPriKey);
        if (!serverSign.equals(clientSign)) {
            ctx.fail(2, "login failed");
            return false;
        }
        
        return true;
    }
    
    private void sendResult(Context ctx, Map<String, Object> result) {
        if (result.containsKey("errcode")) {
            ctx.fail(SignUtil.toInt(result.get("errcode")), (String) result.get("errmsg"));
        } else {
            ctx.ok(result);
        }
    }
    
    // ==================== 房间号生成 ====================
    
    public String generateRoomId() {
        for (int i = 0; i < 100; i++) {
            long counter = roomIdCounter.incrementAndGet();
            long roomNum = 100000 + (counter % 900000);
            String roomId = String.valueOf(roomNum);
            
            String existing = cache.get(KEY_ROOM_SERVER + roomId);
            if (existing == null) {
                return roomId;
            }
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    // ==================== 业务方法 ====================
    
    public Map<String, Object> createPrivateRoom(long userId, String name, String conf, String gameType) {
        // 检查用户是否已在房间中
        String existingRoom = cache.get(KEY_USER_ROOM + userId);
        if (existingRoom != null) {
            return errorResult(-1, "user is playing in room now");
        }
        
        // 选择服务器
        ServerInfo server = roomService.chooseServer(gameType);
        if (server == null) {
            return errorResult(101, "no available server");
        }
        
        // 创建房间
        Map<String, Object> createResult = roomService.createRoom(userId, 0, conf, server);
        if (createResult == null || SignUtil.toInt(createResult.get("errcode")) != 0) {
            int errcode = createResult != null ? SignUtil.toInt(createResult.get("errcode")) : 102;
            return errorResult(errcode, "create failed");
        }
        
        String roomId = (String) createResult.get("roomid");
        cache.set(KEY_ROOM_SERVER + roomId, server.id, 86400);
        
        // 进入房间
        Map<String, Object> enterResult = roomService.enterRoom(userId, name, roomId, server);
        if (enterResult == null || SignUtil.toInt(enterResult.get("errcode")) != 0) {
            return errorResult(102, "enter room failed");
        }
        
        cache.set(KEY_USER_ROOM + userId, roomId, 86400);
        
        // 构造返回结果
        Map<String, Object> ret = new HashMap<>();
        ret.put("roomid", roomId);
        ret.put("ip", server.ip);
        ret.put("port", server.clientport);
        ret.put("gameType", gameType);
        ret.put("token", enterResult.get("token"));
        ret.put("time", System.currentTimeMillis());
        ret.put("sign", sign.sign(roomId, enterResult.get("token"), ret.get("time")));
        return ret;
    }
    
    public Map<String, Object> enterPrivateRoom(long userId, String name, String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            return errorResult(-1, "parameters don't match api requirements");
        }
        
        // 查找房间所在服务器
        String serverId = cache.get(KEY_ROOM_SERVER + roomId);
        ServerInfo server = serverId != null ? roomService.getServerMap().get(serverId) : null;
        
        if (server != null) {
            if (!roomService.isRoomRunning(roomId, server)) {
                server = findRoomInServers(roomId);
            }
        } else {
            server = findRoomInServers(roomId);
        }
        
        if (server == null) {
            return errorResult(-2, "room not found");
        }
        
        // 进入房间
        Map<String, Object> enterResult = roomService.enterRoom(userId, name, roomId, server);
        if (enterResult == null || SignUtil.toInt(enterResult.get("errcode")) != 0) {
            int errcode = enterResult != null ? SignUtil.toInt(enterResult.get("errcode")) : -1;
            String errmsg = enterResult != null ? (String) enterResult.get("errmsg") : "enter room failed";
            return errorResult(errcode, errmsg);
        }
        
        cache.set(KEY_ROOM_SERVER + roomId, server.id, 86400);
        cache.set(KEY_USER_ROOM + userId, roomId, 86400);
        
        // 构造返回结果
        Map<String, Object> ret = new HashMap<>();
        ret.put("roomid", roomId);
        ret.put("ip", server.ip);
        ret.put("port", server.clientport);
        ret.put("gameType", server.gameType);
        ret.put("token", enterResult.get("token"));
        ret.put("time", System.currentTimeMillis());
        ret.put("sign", sign.sign(roomId, enterResult.get("token"), ret.get("time")));
        return ret;
    }
    
    private ServerInfo findRoomInServers(String roomId) {
        for (ServerInfo server : roomService.getServerMap().values()) {
            if (System.currentTimeMillis() - server.lastHeartbeat > 60000) continue;
            if (roomService.isRoomRunning(roomId, server)) {
                return server;
            }
        }
        return null;
    }
    
    public Map<String, Object> getUserRoom(long userId) {
        String roomId = cache.get(KEY_USER_ROOM + userId);
        if (roomId != null) {
            String serverId = cache.get(KEY_ROOM_SERVER + roomId);
            ServerInfo server = serverId != null ? roomService.getServerMap().get(serverId) : null;
            if (server != null) {
                Map<String, Object> ret = new HashMap<>();
                ret.put("roomId", roomId);
                ret.put("ip", server.clientip);
                ret.put("port", server.clientport);
                return ret;
            }
        }
        return null;
    }
    
    public void clearUserRoom(long userId) {
        cache.del(KEY_USER_ROOM + userId);
    }
    
    public void onRoomDestroyed(String roomId) {
        if (roomId != null) {
            cache.del(KEY_ROOM_SERVER + roomId);
            app.log.info("Room destroyed: " + roomId);
        }
    }
    
    public void clearRoom(String roomId) {
        if (roomId != null) {
            cache.del(KEY_ROOM_SERVER + roomId);
        }
    }
    
    private Map<String, Object> errorResult(int code, String msg) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("errcode", code);
        ret.put("errmsg", msg);
        return ret;
    }
}
