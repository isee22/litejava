package game.hall.service;

import game.hall.model.MatchResult;
import game.hall.model.MatchUser;
import game.hall.model.ServerInfo;
import game.hall.util.SignUtil;
import litejava.App;
import litejava.plugins.cache.MemoryCachePlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 匹配服务 (原作没有，本项目扩展)
 * 
 * 职责:
 * - 匹配队列管理
 * - 匹配逻辑处理
 */
public class MatchService {
    
    private static final String KEY_ROOM_SERVER = "hall:room:";
    private static final String KEY_USER_ROOM = "hall:user:";
    
    private final App app;
    private final MemoryCachePlugin cache;
    private final SignUtil sign;
    private final RoomService roomService;
    private final ClientService clientService;
    
    // 匹配队列
    private final Map<String, List<MatchUser>> matchQueues = new ConcurrentHashMap<>();
    private final Map<Long, String> userMatchStatus = new ConcurrentHashMap<>();
    private final Map<Long, MatchResult> matchResults = new ConcurrentHashMap<>();
    private final Map<String, Integer> queueConfigs = new HashMap<>();
    
    public MatchService(App app, MemoryCachePlugin cache, SignUtil sign, 
                        RoomService roomService, ClientService clientService) {
        this.app = app;
        this.cache = cache;
        this.sign = sign;
        this.roomService = roomService;
        this.clientService = clientService;
        
        initQueueConfigs();
    }
    
    private void initQueueConfigs() {
        queueConfigs.put("doudizhu:normal", 3);
        queueConfigs.put("mahjong:normal", 4);
        queueConfigs.put("gobang:normal", 2);
    }
    
    /**
     * 注册路由
     */
    public void setupRoutes() {
        // 开始匹配
        app.post("/match/start", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = ((Number) req.get("userId")).longValue();
            String gameType = (String) req.get("gameType");
            String level = (String) req.getOrDefault("level", "normal");
            String name = (String) req.getOrDefault("name", "玩家" + userId);
            
            Map<String, Object> result = startMatch(userId, gameType, level, name);
            if (result.containsKey("errcode")) {
                ctx.fail(SignUtil.toInt(result.get("errcode")), (String) result.get("errmsg"));
            } else {
                ctx.ok(result);
            }
        });
        
        // 取消匹配
        app.post("/match/cancel", ctx -> {
            Map<String, Object> req = ctx.bindJSON();
            long userId = ((Number) req.get("userId")).longValue();
            cancelMatch(userId);
            ctx.ok(null);
        });
    }
    
    /**
     * 注册管理路由
     */
    public void setupAdminRoutes() {
        // 获取匹配队列状态
        app.get("/admin/match_queues", ctx -> {
            ctx.ok(getQueueStatus());
        });
    }
    
    public Map<String, Integer> getQueueConfigs() {
        return queueConfigs;
    }
    
    public Map<String, Object> startMatch(long userId, String gameType, String level, String name) {
        String queueKey = gameType + ":" + level;
        if (!queueConfigs.containsKey(queueKey)) {
            return errorResult(1, "invalid game type");
        }
        
        // 检查是否已有匹配结果
        MatchResult result = matchResults.remove(userId);
        if (result != null) {
            return result.toMap();
        }
        
        // 检查是否正在匹配 - 如果是，返回 matching 状态而不是错误
        if (userMatchStatus.containsKey(userId)) {
            // 等待匹配结果 (最多5秒)
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                result = matchResults.remove(userId);
                if (result != null) {
                    return result.toMap();
                }
                if (!userMatchStatus.containsKey(userId)) {
                    return Map.of("status", "cancelled");
                }
            }
            return Map.of("status", "matching");
        }
        
        // 加入队列
        MatchUser user = new MatchUser();
        user.userId = userId;
        user.name = name;
        matchQueues.computeIfAbsent(queueKey, k -> new CopyOnWriteArrayList<>()).add(user);
        userMatchStatus.put(userId, queueKey);
        
        // 等待匹配结果 (最多5秒)
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
            result = matchResults.remove(userId);
            if (result != null) {
                return result.toMap();
            }
            if (!userMatchStatus.containsKey(userId)) {
                return Map.of("status", "cancelled");
            }
        }
        return Map.of("status", "matching");
    }
    
    public void cancelMatch(long userId) {
        String queueKey = userMatchStatus.remove(userId);
        if (queueKey != null) {
            List<MatchUser> queue = matchQueues.get(queueKey);
            if (queue != null) {
                queue.removeIf(u -> u.userId == userId);
            }
        }
    }
    
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, List<MatchUser>> e : matchQueues.entrySet()) {
            result.put(e.getKey(), e.getValue().size());
        }
        return result;
    }
    
    /**
     * 处理匹配 (定时调用)
     */
    public void processMatch() {
        for (Map.Entry<String, List<MatchUser>> entry : matchQueues.entrySet()) {
            String queueKey = entry.getKey();
            List<MatchUser> queue = entry.getValue();
            int need = queueConfigs.getOrDefault(queueKey, 4);
            
            if (!queue.isEmpty()) {
                app.log.info("processMatch: " + queueKey + " queue=" + queue.size() + " need=" + need);
            }
            
            while (queue.size() >= need) {
                app.log.info("processMatch: starting match for " + queueKey);
                List<MatchUser> players = new ArrayList<>();
                for (int i = 0; i < need && !queue.isEmpty(); i++) {
                    players.add(queue.remove(0));
                }
                
                String gameType = queueKey.split(":")[0];
                ServerInfo server = roomService.chooseServer(gameType);
                if (server == null) {
                    app.log.warn("processMatch: no server for " + gameType);
                    queue.addAll(0, players);
                    break;
                }
                
                app.log.info("processMatch: found server " + server.id + " for " + gameType);
                
                try {
                    // 创建房间
                    Map<String, Object> createResult = roomService.createRoom(players.get(0).userId, 0, "{}", server);
                    app.log.info("processMatch: createRoom result = " + createResult);
                    if (createResult == null || SignUtil.toInt(createResult.get("errcode")) != 0) {
                        app.log.error("processMatch: createRoom failed");
                        queue.addAll(0, players);
                        break;
                    }
                    
                    String roomId = (String) createResult.get("roomid");
                    cache.set(KEY_ROOM_SERVER + roomId, server.id, 86400);
                    
                    // 所有玩家进入房间
                    for (MatchUser p : players) {
                        Map<String, Object> enterResult = roomService.enterRoom(p.userId, p.name, roomId, server);
                        if (enterResult != null && SignUtil.toInt(enterResult.get("errcode")) == 0) {
                            cache.set(KEY_USER_ROOM + p.userId, roomId, 86400);
                            userMatchStatus.remove(p.userId);
                            
                            MatchResult r = new MatchResult();
                            r.status = "matched";
                            r.roomId = roomId;
                            r.ip = server.ip;
                            r.port = server.clientport;
                            r.gameType = gameType;
                            r.token = String.valueOf(enterResult.get("token"));
                            matchResults.put(p.userId, r);
                        }
                    }
                    app.log.info("Match success: " + queueKey + " -> " + roomId);
                } catch (Exception e) {
                    app.log.error("Match failed: " + e.getMessage());
                    queue.addAll(0, players);
                    break;
                }
            }
        }
    }
    
    private Map<String, Object> errorResult(int code, String msg) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("errcode", code);
        ret.put("errmsg", msg);
        return ret;
    }
}
