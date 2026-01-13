package game.hall.service;

import game.common.ErrCode;
import game.common.GameException;
import game.hall.G;
import game.hall.cache.CacheKeys;
import game.hall.model.ServerInfo;
import game.hall.util.HttpUtil;
import game.hall.util.SignUtil;
import game.hall.vo.RoomInfoVO;
import game.hall.vo.RoomResultVO;
import litejava.App;
import litejava.plugins.cache.RedisCachePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 房间服务
 */
public class RoomService {
    
    public App app = G.app;
    public RedisCachePlugin cache = G.cache;
    public ServerRegistry registry = G.registry;
    
    public RoomResultVO quickStart(long userId, String name, String gameType, String conf) {
        RoomResultVO existing = checkAndReconnect(userId);
        if (existing != null) return existing;
        
        return cache.withLock(CacheKeys.lockQuickStart(gameType), CacheKeys.LOCK_EXPIRE, () -> {
            RoomResultVO result = tryJoinAvailable(userId, name, gameType);
            if (result != null) return result;
            return doCreateRoom(userId, name, gameType, conf, true);
        });
    }
    
    public RoomResultVO createRoom(long userId, String name, String gameType, String conf) {
        String existingRoom = cache.get(CacheKeys.user(userId));
        if (existingRoom != null) {
            GameException.error(ErrCode.ROOM_ALREADY_IN, "user is playing in room now");
        }
        return doCreateRoom(userId, name, gameType, conf, false);
    }
    
    public RoomResultVO joinRoom(long userId, String name, String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            GameException.error(ErrCode.INVALID_ACTION, "roomId is required");
        }
        
        RoomInfoVO roomInfo = getRoomInfo(roomId);
        if (roomInfo == null) {
            GameException.error(ErrCode.ROOM_NOT_FOUND, "room not found");
        }
        
        ServerInfo server = registry.get(roomInfo.serverId);
        if (server == null) {
            GameException.error(ErrCode.ROOM_NOT_FOUND, "server offline");
        }
        
        Map<String, Object> result = callEnterRoom(userId, name, roomId, server);
        if (result == null || SignUtil.toInt(result.get("errcode")) != 0) {
            int errcode = result != null ? SignUtil.toInt(result.get("errcode")) : ErrCode.UNKNOWN;
            String errmsg = result != null ? (String) result.get("errmsg") : "enter room failed";
            GameException.error(errcode, errmsg);
        }
        
        recordUserRoom(userId, roomId);
        return buildResult(roomId, server, String.valueOf(result.get("token")));
    }

    public RoomResultVO reconnect(long userId) {
        RoomResultVO result = checkAndReconnect(userId);
        if (result == null) {
            GameException.error(ErrCode.ROOM_NOT_FOUND, "no room to reconnect");
        }
        return result;
    }
    
    public RoomResultVO getUserRoom(long userId) {
        String roomId = cache.get(CacheKeys.user(userId));
        if (roomId == null) return null;
        
        RoomInfoVO roomInfo = getRoomInfo(roomId);
        if (roomInfo == null) return null;
        
        ServerInfo server = registry.get(roomInfo.serverId);
        if (server == null) return null;
        
        RoomResultVO vo = new RoomResultVO();
        vo.roomId = roomId;
        vo.ip = server.clientip;
        vo.port = server.clientport;
        vo.wsUrl = "ws://" + server.clientip + ":" + server.clientport + "/game";
        vo.gameType = roomInfo.gameType;
        return vo;
    }
    
    public List<RoomInfoVO> listJoinableRooms(String gameType, int limit) {
        List<RoomInfoVO> rooms = new ArrayList<>();
        Set<String> roomIds = cache.smembers(CacheKeys.joinableRooms(gameType));
        if (roomIds == null) return rooms;
        
        int count = 0;
        for (String roomId : roomIds) {
            if (count >= limit) break;
            RoomInfoVO info = getRoomInfo(roomId);
            if (info != null && info.joinable && info.playerCount < info.maxPlayers) {
                rooms.add(info);
                count++;
            }
        }
        return rooms;
    }
    
    public void onUserExit(long userId) {
        String roomId = cache.get(CacheKeys.user(userId));
        if (roomId != null) {
            cache.srem(CacheKeys.roomUsers(roomId), String.valueOf(userId));
        }
        cache.del(CacheKeys.user(userId));
    }
    
    public void onRoomDestroyed(String roomId) {
        if (roomId == null) return;
        
        RoomInfoVO roomInfo = getRoomInfo(roomId);
        if (roomInfo != null) {
            cache.srem(CacheKeys.serverRooms(roomInfo.serverId), roomId);
            cache.srem(CacheKeys.joinableRooms(roomInfo.gameType), roomId);
        }
        
        Set<String> userIds = cache.smembers(CacheKeys.roomUsers(roomId));
        if (userIds != null) {
            for (String odUserId : userIds) {
                cache.del(CacheKeys.user(Long.parseLong(odUserId)));
            }
        }
        
        cache.del(CacheKeys.room(roomId));
        cache.del(CacheKeys.roomUsers(roomId));
        cache.del(CacheKeys.available(roomInfo != null ? roomInfo.gameType : "default"));
        
        app.log.info("房间销毁: " + roomId);
    }
    
    public void updateRoomState(String roomId, int playerCount, boolean joinable) {
        RoomInfoVO info = getRoomInfo(roomId);
        if (info == null) return;
        
        info.playerCount = playerCount;
        info.joinable = joinable;
        saveRoomInfo(info);
        
        if (joinable && playerCount < info.maxPlayers) {
            cache.sadd(CacheKeys.joinableRooms(info.gameType), roomId);
        } else {
            cache.srem(CacheKeys.joinableRooms(info.gameType), roomId);
        }
    }

    // ==================== 私有方法 ====================
    
    private RoomResultVO checkAndReconnect(long userId) {
        String roomId = cache.get(CacheKeys.user(userId));
        if (roomId == null) return null;
        
        RoomInfoVO roomInfo = getRoomInfo(roomId);
        if (roomInfo == null) {
            cache.del(CacheKeys.user(userId));
            return null;
        }
        
        ServerInfo server = registry.get(roomInfo.serverId);
        if (server == null || !isRoomRunning(roomId, server)) {
            cache.del(CacheKeys.user(userId));
            return null;
        }
        
        RoomResultVO vo = new RoomResultVO();
        vo.roomId = roomId;
        vo.wsUrl = "ws://" + server.clientip + ":" + server.clientport + "/game";
        vo.ip = server.clientip;
        vo.port = server.clientport;
        vo.gameType = roomInfo.gameType;
        vo.reconnect = true;
        return vo;
    }
    
    private RoomResultVO tryJoinAvailable(long userId, String name, String gameType) {
        String roomId = cache.get(CacheKeys.available(gameType));
        if (roomId == null) return null;
        
        RoomInfoVO roomInfo = getRoomInfo(roomId);
        if (roomInfo == null) {
            cache.del(CacheKeys.available(gameType));
            return null;
        }
        
        ServerInfo server = registry.get(roomInfo.serverId);
        if (server == null || !isRoomRunning(roomId, server)) {
            cache.del(CacheKeys.available(gameType));
            return null;
        }
        
        Map<String, Object> result = callEnterRoom(userId, name, roomId, server);
        if (result == null || SignUtil.toInt(result.get("errcode")) != 0) {
            cache.del(CacheKeys.available(gameType));
            return null;
        }
        
        recordUserRoom(userId, roomId);
        return buildResult(roomId, server, String.valueOf(result.get("token")));
    }
    
    private RoomResultVO doCreateRoom(long userId, String name, String gameType, String conf, boolean markAvailable) {
        ServerInfo server = registry.choose(gameType);
        if (server == null) {
            GameException.error(ErrCode.NO_SERVER, "no available server for " + gameType);
        }
        
        Map<String, Object> createResult = callCreateRoom(userId, conf, server);
        if (createResult == null || SignUtil.toInt(createResult.get("errcode")) != 0) {
            int errcode = createResult != null ? SignUtil.toInt(createResult.get("errcode")) : ErrCode.UNKNOWN;
            String errmsg = createResult != null ? (String) createResult.get("errmsg") : "create failed";
            GameException.error(errcode, errmsg);
        }
        
        String roomId = (String) createResult.get("roomid");
        int maxPlayers = SignUtil.toInt(createResult.get("maxPlayers"));
        if (maxPlayers <= 0) maxPlayers = 4;
        
        RoomInfoVO roomInfo = new RoomInfoVO();
        roomInfo.roomId = roomId;
        roomInfo.serverId = server.id;
        roomInfo.gameType = gameType;
        roomInfo.ownerId = userId;
        roomInfo.playerCount = 0;
        roomInfo.maxPlayers = maxPlayers;
        roomInfo.joinable = true;
        roomInfo.createTime = System.currentTimeMillis();
        saveRoomInfo(roomInfo);
        
        cache.sadd(CacheKeys.serverRooms(server.id), roomId);
        
        Map<String, Object> enterResult = callEnterRoom(userId, name, roomId, server);
        if (enterResult == null || SignUtil.toInt(enterResult.get("errcode")) != 0) {
            GameException.error(ErrCode.UNKNOWN, "enter room failed");
        }
        
        recordUserRoom(userId, roomId);
        
        if (markAvailable) {
            cache.set(CacheKeys.available(gameType), roomId, CacheKeys.ROOM_EXPIRE);
            cache.sadd(CacheKeys.joinableRooms(gameType), roomId);
        }
        
        return buildResult(roomId, server, String.valueOf(enterResult.get("token")));
    }
    
    private void recordUserRoom(long userId, String roomId) {
        cache.set(CacheKeys.user(userId), roomId, CacheKeys.ROOM_EXPIRE);
        cache.sadd(CacheKeys.roomUsers(roomId), String.valueOf(userId));
    }
    
    private RoomInfoVO getRoomInfo(String roomId) {
        String json = cache.get(CacheKeys.room(roomId));
        if (json == null) return null;
        return app.json.parse(json, RoomInfoVO.class);
    }
    
    private void saveRoomInfo(RoomInfoVO info) {
        cache.set(CacheKeys.room(info.roomId), app.json.stringify(info), CacheKeys.ROOM_EXPIRE);
    }
    
    private boolean isRoomRunning(String roomId, ServerInfo server) {
        try {
            String url = "http://" + server.ip + ":" + server.httpPort + "/is_room_runing?roomid=" + roomId;
            String resp = HttpUtil.get(url);
            if (resp == null || resp.isEmpty()) return false;
            
            Map<String, Object> data = app.json.parseMap(resp);
            return data != null && SignUtil.toInt(data.get("errcode")) == 0 && Boolean.TRUE.equals(data.get("runing"));
        } catch (Exception e) {
            return false;
        }
    }
    
    private Map<String, Object> callCreateRoom(long userId, String conf, ServerInfo server) {
        String url = "http://" + server.ip + ":" + server.httpPort + "/create_room"
            + "?userid=" + userId + "&conf=" + HttpUtil.encode(conf) + "&sign=" + SignUtil.sign(userId, conf);
        String resp = HttpUtil.get(url);
        return app.json.parseMap(resp);
    }
    
    private Map<String, Object> callEnterRoom(long userId, String name, String roomId, ServerInfo server) {
        String url = "http://" + server.ip + ":" + server.httpPort + "/enter_room"
            + "?userid=" + userId + "&name=" + HttpUtil.encode(name) + "&roomid=" + roomId 
            + "&sign=" + SignUtil.sign(userId, name, roomId);
        String resp = HttpUtil.get(url);
        return app.json.parseMap(resp);
    }
    
    private RoomResultVO buildResult(String roomId, ServerInfo server, String token) {
        RoomResultVO vo = new RoomResultVO();
        vo.roomId = roomId;
        vo.token = token;
        vo.wsUrl = "ws://" + server.clientip + ":" + server.clientport + "/game";
        vo.ip = server.clientip;
        vo.port = server.clientport;
        vo.gameType = server.gameType;
        return vo;
    }
}
